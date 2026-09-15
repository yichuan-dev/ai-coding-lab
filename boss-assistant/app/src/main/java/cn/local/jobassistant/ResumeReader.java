package cn.local.jobassistant;

import android.content.Context;
import com.tom_roush.pdfbox.pdmodel.PDDocument;
import com.tom_roush.pdfbox.text.PDFTextStripper;
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader;
import org.xmlpull.v1.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.zip.*;

public final class ResumeReader {
    public static byte[] bounded(InputStream in,int max) throws IOException {
        ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] a=new byte[8192];int n;
        while((n=in.read(a))!=-1) { if(b.size()+n>max)throw new IOException("文件过大");b.write(a,0,n); } return b.toByteArray();
    }
    public static String parse(Context context,String name,byte[] bytes) throws Exception {
        if(bytes.length>5*1024*1024)throw new IOException("简历最大 5 MB");String result;
        if(name.toLowerCase().endsWith(".pdf")) {
            PDFBoxResourceLoader.init(context);
            try(PDDocument d=PDDocument.load(bytes)) { if(d.getNumberOfPages()>30)throw new IOException("简历最多 30 页");result=new PDFTextStripper().getText(d); }
        } else if(name.toLowerCase().endsWith(".docx")) {
            result="";int count=0;
            try(ZipInputStream z=new ZipInputStream(new ByteArrayInputStream(bytes))) { ZipEntry e;
                while((e=z.getNextEntry())!=null) {
                    if(++count>1500)throw new IOException("DOCX 内容过多");
                    if(!e.getName().equals("word/document.xml"))continue;
                    byte[] xml=bounded(z,5*1024*1024);String s=new String(xml,StandardCharsets.UTF_8);
                    if(s.contains("<!DOCTYPE")||s.contains("<!ENTITY"))throw new IOException("DOCX 包含不支持的外部实体");
                    XmlPullParser x=XmlPullParserFactory.newInstance().newPullParser();x.setInput(new StringReader(s));StringBuilder out=new StringBuilder();
                    for(int t=x.getEventType();t!=XmlPullParser.END_DOCUMENT;t=x.next()) { if(t==XmlPullParser.TEXT)out.append(x.getText());if(t==XmlPullParser.END_TAG && x.getName().endsWith("p"))out.append('\n'); }
                    result=out.toString();break;
                }
            }
        } else if(name.toLowerCase().endsWith(".txt")) result=new String(bytes,StandardCharsets.UTF_8);
        else throw new IOException("请选择 PDF、DOCX 或 TXT 简历");
        if(result.isBlank())throw new IOException("未提取到文字；扫描 PDF 请先转换为文字版");
        if(result.length()>60000)throw new IOException("简历文字过长");return result.trim();
    }
}
