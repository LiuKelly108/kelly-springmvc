package com.kelly.demo.mvc.v3;

import com.kelly.demo.mvc.annotation.*;
import com.kelly.demo.mvc.util.StringUtil;
import org.omg.PortableInterceptor.LOCATION_FORWARD;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class KYDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";
    private Properties contextConfig = new Properties();//配置文件
    //存放类的类的IOC
    private List<String> classNameList = new ArrayList<String>();
    //存放bean的IOC
    private ConcurrentHashMap<String, Object> ioc = new ConcurrentHashMap<String, Object>();
    //存放controller的方法
    private List<Handler> handlerMapperList  =new ArrayList<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String result = "";
        try {
           result =doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:" + Arrays.toString(e.getStackTrace()));
        }
        resp.getWriter().write(result);
    }


    @Override
    public void init(ServletConfig config) throws ServletException {
        System.out.println("------开始初始化framework-------");
        long startTime=System.currentTimeMillis();
        //1、加载web.xml中配置的application.properties到内存中
        loadServletConfig(config.getInitParameter(LOCATION));
        //2、扫描application.properties中配置package下的所有的类
        scanPackage(contextConfig.getProperty("scanPackage"));
        //3、初始化扫描到的类，将其放入到IOC容器中
        doInstanceClass();
        //4、完成依赖注入
        autoWired();
        //5、初始化handlerMapping
        initHandlerMapping();

        long endTime=System.currentTimeMillis();
        System.out.println("------framework初始化完成------共耗费时间："+(endTime-startTime)+"ms");

    }
    /**
     * 转发
     *
     * @param req
     * @param resp
     */
    private String doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        //根据req获得Handler
        Handler handler = getHandlerByReq(req);
        if(null == handler){
            resp.getWriter().write("404 NOT FOUND!");
            return  null ;
        }
       //根据url获得方法
       Method method = handler.getMethod();
        //获取方法的参数列表
        Class<?> [] paramTypes = handler.getMethod().getParameterTypes();
        //保存所有需要自动赋值的参数值
        Object [] paramValues = new Object[paramTypes.length];
        //req的参数集合
        Map<String,String[]> params = req.getParameterMap();
        //变量req参数集合进行匹配
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");

            //如果找到匹配的对象，则开始填充参数值
            if(!handler.paramIndexMapping.containsKey(param.getKey())){continue;}
            int index = handler.paramIndexMapping.get(param.getKey());
            paramValues[index] = convert(paramTypes[index],value);
        }

        //设置方法中的request和response对象
        int reqIndex = handler.paramIndexMapping.get(HttpServletRequest.class.getName());
        paramValues[reqIndex] = req;
        int respIndex = handler.paramIndexMapping.get(HttpServletResponse.class.getName());
        paramValues[respIndex] = resp;

        //反射调用
        String str = "";
        try {
            str =(String) handler.getMethod().invoke(handler.getController(), paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return  str ;
    }

    /**
     * 根据req 获得返回的handler
     * @param req
     * @return
     */
    private Handler getHandlerByReq(HttpServletRequest req) {
        if(handlerMapperList.isEmpty()){
            return null;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        for (Handler handler : handlerMapperList) {
            try{
                Matcher matcher = handler.getPattern().matcher(url);
                //如果没有匹配的，再进行下一个匹配
                if(!matcher.matches()){ continue; }

                return handler;
            }catch(Exception e){
                throw e;
            }
        }
        return null;
    }

    //将value转换成Type类型
    private Object convert(Class<?> type,String value){
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        return value;
    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void loadServletConfig(String contextConfigLocation) {

        InputStream is = null ;
        //获得contextConfigLocation的输入流

        try {
            is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
            //将contextConfigLocation的内容加载到成员变量contextConfig中
            contextConfig.load(is);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if (is != null ){
                try {
                    is.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 扫描包中的类
     * @param scanPackage
     */
    private void  scanPackage(String scanPackage) {
        // String scanPackage=contextConfig.getProperty("scanPackage");
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.", "/"));
        //通过url获得文件路径
        File filePath = new File(url.getFile());
        //通过文件路径，遍历文件列表
        for (File file : filePath.listFiles()) {
            if (file.isDirectory()) {//如果file是目录
                scanPackage(scanPackage + "." + file.getName());
            } else {
                //查找文件为.class结尾的
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                String className = file.getName().replace(".class", "");
                classNameList.add(scanPackage + "." + className);
            }
        }
    }


    /*
     * 实例化类，将类@KYController和KYService注解的类放到IOC容器中
     */
    private void doInstanceClass() {
        if(classNameList.isEmpty()){
            return  ;
        }
        //遍历保存的classNameList,将其放入到ioc中
        try {
            for (String className:classNameList) {
                Class<?> clazz =  Class.forName(className);
                //加了注解的类才进行初始化
                //1、action功能的类（@KYController）
                if(clazz.isAnnotationPresent(KYController.class)){
                    Object obj =  clazz.newInstance();
                    //spring默认类名为首字母小写
                    String beanName = StringUtil.toFirstLowerCase(clazz.getSimpleName());
                    ioc.put(beanName,obj);
                }else if(clazz.isAnnotationPresent(KYService.class)){
                    String beanName=StringUtil.toFirstLowerCase(clazz.getSimpleName());
                    KYService kyService = clazz.getAnnotation(KYService.class);
                    if(!"".equals(kyService.value())){
                        beanName=kyService.value();
                    }
                    Object obj = clazz.newInstance();
                    ioc.put(beanName,obj);

                }else{
                    continue;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 依赖注入
     */
    private void autoWired() {
        if(ioc.isEmpty()){
            return;
        }
        for(Map.Entry<String,Object>  entry: ioc.entrySet()){
            //拿到成员变量
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:fields){
                if(!field.isAnnotationPresent(KYAutowired.class)){
                    continue;
                }
                KYAutowired kyAutowired = field.getAnnotation(KYAutowired.class);
                String beanName = kyAutowired.value().trim();
                if("".equals(beanName)){
                    beanName=StringUtil.toFirstLowerCase(field.getType().getSimpleName());
                }
                field.setAccessible(true);
                try {
                    //执行注入动作
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                    continue;
                }
            }
        }
    }


    /**
     * 将@KYcontroller注解类的方法进行映射
     */
    private void initHandlerMapping() {
        if(ioc.isEmpty()){
            return;
        }

        for(Map.Entry<String,Object> entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(KYController.class)){
                continue;
            }
            //获得类上的@KYRequestMapping ,
            String baseUrl="";
            if(clazz.isAnnotationPresent(KYRequestMapping.class)){
                KYRequestMapping kyRequestMapping = clazz.getAnnotation(KYRequestMapping.class);
                baseUrl = kyRequestMapping.value();
            }
            Method[] methods =clazz.getMethods();
            for(Method method:methods){
                if(!method.isAnnotationPresent(KYRequestMapping.class)){
                    continue;
                }
                KYRequestMapping kyRequestMapping = method.getAnnotation(KYRequestMapping.class);
                String url = kyRequestMapping.value();
                Pattern regex = Pattern.compile(baseUrl+url);
                handlerMapperList.add(new Handler(regex,entry.getValue(),method));
                System.out.println("Mapped url:"+(baseUrl+url)+"——>"+method);
            }
        }
    }

}


