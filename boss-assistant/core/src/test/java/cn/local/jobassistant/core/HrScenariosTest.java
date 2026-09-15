package cn.local.jobassistant.core;

import org.junit.*;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.json.*;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class HrScenariosTest {
    @Parameterized.Parameters(name="{0}") public static Collection<Object[]> cases(){return Arrays.asList(new Object[][]{
        {"还在找工作吗？","求职状态",true,false},{"方便聊聊吗？","问候",false,false},{"可以发一下简历吗？","简历",true,true},{"你什么时候毕业？","毕业时间",false,false},{"有Java经验吗？","技能",false,false},{"最快什么时候到岗？","到岗",true,true},{"期望工资多少？","薪资",false,true},{"可以接受加班吗？","加班",true,false},{"什么时候方便面试？","面试",true,true},{"方便加微信吗？","联系方式",true,true},{"有相关项目经验吗？","项目",false,false},{"可以提供身份证信息吗？","敏感资料",true,true}
    });}
    private final String q,cat;private final boolean hold,important;
    public HrScenariosTest(String q,String cat,boolean hold,boolean important){this.q=q;this.cat=cat;this.hold=hold;this.important=important;}
    @Test public void classifyAndUseOnlyApprovedTruth(){JSONObject facts=J.obj("毕业时间",J.obj("category","毕业时间","answer","我计划于明年六月毕业。"),"技能",J.obj("category","技能","answer","我学习过 Java 基础。"),"薪资",J.obj("category","薪资","answer","我期望月薪八千元。"),"项目",J.obj("category","项目","answer","我做过课程作业项目。"));ReplyPolicy.Decision d=ReplyPolicy.decide(q,facts,cat,false,false);assertEquals(cat,d.category());assertEquals(hold,d.holding());if(important)assertTrue(d.notifyUser());assertTrue(d.allowed());assertFalse(d.text().contains("AI"));assertTrue(d.text().length()<100);}
}
