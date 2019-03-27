package com.kelly.demo.mvc.util;

public class StringUtil {

    /**
     * 字符串的首字母小写
     * @param str
     * @return
     */
    public static String toFirstLowerCase(String str){
        //之所以加，是因为大小写字母的ASCII码相差32，
        // 而且大写字母的ASCII码要小于小写字母的ASCII码
        //在Java中，对char做算学运算，实际上就是对ASCII码做算学运算
        char[] chars = str.toCharArray();
        chars[0]+=32 ; //大写+32=小写
        return String.valueOf(chars) ;
    }
}
