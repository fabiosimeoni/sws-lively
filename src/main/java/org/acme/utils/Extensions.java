package org.acme.utils;

import java.util.ArrayList;
import java.util.List;

import lombok.experimental.UtilityClass;

import org.fao.sws.model.Domain;
import org.fao.sws.model.dao.impl.mapping.DataProvider;
import org.fao.sws.model.filter.DimensionFilter;

@UtilityClass
public class Extensions {

	public DimensionFilter filter(Domain domain) {
		DimensionFilter filter = new DimensionFilter().setDomainCode(domain.id());
		
		domain.datasets().stream().findFirst().ifPresent(ds->{
			filter.setDataSetCode(ds.id());
			ds.dimensions().stream().findFirst().ifPresent(d->filter.setDimensionCode(d.id()));
		});
		
		return filter;
	}


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
