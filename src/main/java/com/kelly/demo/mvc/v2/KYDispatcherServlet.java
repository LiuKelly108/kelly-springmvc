package com.kelly.demo.mvc.v2;

import com.kelly.demo.mvc.annotation.*;
import com.kelly.demo.mvc.util.StringUtil;

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

public class KYDispatcherServlet extends HttpServlet {

    private static final String LOCATION = "contextConfigLocation";
    //保存application.properties的内容
    private Properties contextConfig = new Properties();
    //用于存储包下所有的类名字
    private List<String> classNameList = new ArrayList<String>();
    //存放bean的容器ioc
    private ConcurrentHashMap<String, Object> ioc = new ConcurrentHashMap<String, Object>();
    //存放controller的方法对应关系（url,method）
    private ConcurrentHashMap<String, Method> handlerMapping = new ConcurrentHashMap<String, Method>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            doDispatcher(req, resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception Detail:" + Arrays.toString(e.getStackTrace()));
        }
    }

    /**
     * 转发
     *
     * @param req
     * @param resp
     */
    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath, "").replaceAll("/+", "/");
        if (!handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 not found!");
            return;
        }
        //通过url获得方法
        Method method = handlerMapping.get(url);

        //获得方法的形参列表
        Class<?>[] parameterType = method.getParameterTypes();
        //获得url的参数列表<参数名,参数值[]>
        Map<String, String[]> paramMap = req.getParameterMap();
        String beanName = StringUtil.toFirstLowerCase(method.getDeclaringClass().getSimpleName());

        //保存从url解析出来的参数
        Object[] paramValues = new Object[parameterType.length];
        //根据参数动态赋值
        for (int i = 0; i < parameterType.length; i++) {
            Class parameterClass = parameterType[i];
            if (parameterClass == HttpServletRequest.class) {
                paramValues[i] = req;
                continue;
            } else if (parameterClass == HttpServletResponse.class) {
                paramValues[i] = resp;
            } else if (parameterClass == String.class) {
                //方法中参数注解
                boolean flg = method.isAnnotationPresent(KYRequestParam.class);

                //取到方法的参数注解
                Annotation[][] annotations = method.getParameterAnnotations();
                for (int j = 0; j < annotations.length; j++) {//参数的个数

                    for (Annotation annotation : annotations[j]) {//每个参数的注解数目
                        if (annotation instanceof KYRequestParam) {
                            //取到注解的值
                            String requestParamValue = ((KYRequestParam) annotation).value();
                            //判断url中是key
                            if (paramMap.containsKey(requestParamValue)) {
                                for (Map.Entry<String, String[]> stringEntry : paramMap.entrySet()) {
                                    //取出url的key对应的value
                                    String value = Arrays.toString(stringEntry.getValue())
                                            .replaceAll("\\[|\\]", "")
                                            .replaceAll("\\s", ",");
                                    paramValues[i] = value;
                                }

                            }
                        }
                    }


                }

            } else if (parameterClass == Integer.class) {
                //取到方法的参数注解
                Annotation[][] annotations = method.getParameterAnnotations();
                for (Annotation annotation : annotations[i]) {  //每个参数的注解数目
                    if (annotation instanceof KYRequestParam) {
                        //取到注解的值
                        String requestParamValue = ((KYRequestParam) annotation).value();
                        //判断url中是key
                        if (paramMap.containsKey(requestParamValue)) {
                            //获得url的key中的值
                            String[] valueArray = paramMap.get(requestParamValue);
                            String value = Arrays.toString(valueArray)
                                    .replaceAll("\\[|\\]", "")
                                    .replaceAll("\\s", ",");
                            paramValues[i] = Integer.valueOf(value);
                        }
                    }
                }

            }

        }
        try {
            //反射调用
            method.invoke(ioc.get(beanName),paramValues);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                handlerMapping.put(baseUrl+url,method);
                System.out.println("Mapped url:"+(baseUrl+url)+"——>"+method);
            }


        }


    }

    /**
     *
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

//                    for(Class<?> clz :clazz.getInterfaces()){
//                        if(ioc.containsKey(clz.getName())){
//                            throw  new Exception("the bean has already exists");
//                        }
//                        //将类的接口也放入的ioc中
//                        ioc.put(StringUtil.toFirstLowerCase(clz.getSimpleName()),clz.getClass());
//                    }

                }else{
                    continue;
                }

            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * 扫描包下所有的类，将类名保存在classNameList中
     * @param scanPackage
     */
    private void scanPackage(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        //通过url获得文件路径
        File filePath = new File(url.getFile());
        //通过文件路径，遍历文件列表
        for(File file:filePath.listFiles()){
            if(file.isDirectory()){//如果file是目录
                scanPackage(scanPackage+"."+file.getName());
            }else{
                //查找文件为.class结尾的
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className = file.getName().replace(".class","");
                classNameList.add(scanPackage+"."+className);
            }
        }

    }

    /**
     * 加载配置文件
     * @param contextConfigLocation
     */
    private void loadServletConfig(String contextConfigLocation) {

        InputStream is = null ;
        //获得contextConfigLocation的输入流
        is = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
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
}
