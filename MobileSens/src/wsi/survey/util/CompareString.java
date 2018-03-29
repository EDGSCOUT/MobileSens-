package wsi.survey.util;


import java.util.Comparator;

public class CompareString implements Comparator<Object> {

	@Override
	public int compare(Object obj1, Object obj2) {
		String str1 = (String)obj1;
		String str2 = (String)obj2;
		
		return str1.compareTo(str2);
	}


}
