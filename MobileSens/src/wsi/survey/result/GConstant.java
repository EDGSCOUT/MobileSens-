package wsi.survey.result;

import android.content.Context;
import android.telephony.TelephonyManager;
import wsi.mobilesens.R;


public class GConstant {
	
	public static String IMEI = "000000000000000";
	public static int deviceScreenWidth = 480;
	public static int deviceScreenHeight = 800;
	public static int titleFontSize = 24;
	
	
	public final static String surveyFileFolder = "surveyfiles"; // 问卷文件夹，注意：文件名称必须与下面二维数组完全一致（特别是字母大小写）
	public final static int[] imgs = { R.drawable.img01, R.drawable.img02,
			R.drawable.img03, R.drawable.img04, R.drawable.img05, R.drawable.img06, R.drawable.img06};
	public final static int[] imgs_completed = { R.drawable.img01_completed, R.drawable.img02_completed,
		R.drawable.img03_completed, R.drawable.img04_completed, R.drawable.img05_completed,  R.drawable.img06_completed,};
	public final static String[][] surveyFiles = {
			{
					"BasicInfo_Phone.xml",
					"个人基本信息",
					"    欢迎您填写本测评，本测评涉及到的所有信息及填写结果我们将严格保密，请您放心填写。\n    填写说明：本测评共有7套问卷，问卷需要您依次填写并提交结果，答题过程中若有未提交成功的问卷结果，请您在问卷选择界面点击菜单键，并重新提交。\n	    数据上传要求：手机充电时请您打开wifi或GPRS，以保证数据上传，谢谢您的配合！" },
			{
					"BFI_Phone.xml",
					"大五人格（BFI）量表",
					"	大五人格量表（The Big Five Inventory, BFI）是根据国际心理学界著名的大五人格理论而创制的人格测量工具，拥有良好的使用效果和坚实的科学基础。它将从五个方面来评估您的个性心理特征，可以让您更好地掌握自己的“心理密码”。" },
			{
					"CES-D_Phone.xml",
					"流调中心抑郁焦虑量表",
					"	焦虑是一种比较普遍的精神体验，长期存在焦虑反应的人易发展为焦虑症。本量表的题目主要描述了您可能存在的或最近有过的感受，请您认真阅读题目并在每一道题目后点击选择合适的数字来表示在【最近一周来】您产生这种感受的频繁度。" },
			{
					"IAS_Phone.xml",
					"交往焦虑量表（IAS）",
					"	本量表是Leary于1983年编制，主要用于测量个体关于社交情境中主观焦虑体验的倾向。本量表对于区分主观上的焦虑和外在的行为表现是有帮助的，对于评估那些害怕人际交往的个体的焦虑程度是一种实用的工具。" },		
			{
					"PWB_Phone.xml",
					"心理幸福感量表（PWB）",
					"	心理幸福感量表是对主观心理幸福感的一种评估，其测评结果将协助您从 不同维度了解自己的心理状态，从而评测您的心理幸福感。" },
			{
					"UCLAAl_Phone.xml",
					"孤独感测试量表", 
					"	欢迎您填写本问卷，下面的题目列出了人们在日常社会交往中可能会出现的一些感受，对每项描述，请指出您在具有此种感受的频度，选择您认同的选项。请您在不受外界打扰的情况下，认真、真实地填写完成本问卷。\n	这是本套问卷最后一次测评，请您确保将每套问卷的测评结果提交。如果测评过程中提交失败，还请您返回主界面查看菜单键并重新提交，非常感谢您的配合！" }
					
		
	};

	/**
	 * author：qinning
	 * 全局变量，记录某一问卷是否已经填写完，如果已经填写完，则对应的问卷position将会被设置为true，否则为false，默认进入应用时第一个为true，其他为false
	 */
	public static boolean[] flags=new boolean[surveyFiles.length];
	
	
	public static void adjustFontSize(int screenWidth, int screenHeight) {
		deviceScreenWidth = screenWidth;
		deviceScreenHeight = screenHeight;

		if (deviceScreenWidth <= 240) { // 240X320 屏幕
			titleFontSize = 10;

		} else if (deviceScreenWidth <= 320) { // 320X480 屏幕
			titleFontSize = 14;

		} else if (deviceScreenWidth <= 480) { // 480X800 或 480X854 屏幕
			titleFontSize = 24;

		} else if (deviceScreenWidth <= 540) { // 540X960 屏幕
			titleFontSize = 26;

		} else if (deviceScreenWidth <= 800) { // 800X1280 屏幕
			titleFontSize = 30;

		} else { // 大于 800X1280
			titleFontSize = 32;

		}
	}
}
