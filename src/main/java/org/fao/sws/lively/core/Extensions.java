package org.fao.sws.lively.core;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

import org.fao.sws.model.dao.impl.mapping.DataProvider;

@UtilityClass
public class Extensions {


	public boolean isEmpty(DataProvider<?> p) {
		
		return p.next()==null;
		
	}
	
	public <T> List<T> toList(DataProvider<T> p) {
		
		List<T> els = new ArrayList<>();
		T el;
		
		while((el=p.next())!=null)
			els.add(el);
		
		return els;
		
	}
}
