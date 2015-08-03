package org.fao.sws.lively.modules;

import java.util.function.Predicate;
import java.util.stream.Stream;

import javax.enterprise.event.Observes;

import lombok.experimental.UtilityClass;

import org.fao.sws.domain.plain.reference.Language;
import org.fao.sws.domain.plain.reference.MetadataElementType;
import org.fao.sws.domain.plain.reference.MetadataType;
import org.fao.sws.ejb.LanguageService;
import org.fao.sws.ejb.MetadataTypeService;
import org.fao.sws.lively.SwsTest.Start;

@UtilityClass
public class Metadata extends Common {
		
		void startup(@Observes Start e, 
				
								MetadataTypeService typeservice,
								LanguageService languageservice) {
			
			Metadata.mdtypeservice = typeservice;
			Metadata.languageservice = languageservice;
		}
		
		
	public MetadataTypeService mdtypeservice;	
	public LanguageService languageservice;	
	
	//////////////// types /////////////////////////////////////////////////////////////////////////
	
	// many
		
	public Stream<MetadataType> metadataTypes() {
		return shuffle(mdtypeservice.findAll());
	}
	
	public Stream<MetadataType> metadataTypesThat(Predicate<MetadataType> predicate) {
		return metadataTypes().filter(predicate);
	}
	
	// one
	
	public MetadataType aMetadataType() {
		return oneof(metadataTypes());
	}
	
	public MetadataType aMetadataTypeThat(Predicate<MetadataType> predicate) {
		return oneof(metadataTypesThat(predicate));
	}
	
	
	////////////////types /////////////////////////////////////////////////////////////////////////
	
	// many
	
	public Stream<MetadataElementType> metadataElementTypes() {
		return shuffle(mdtypeservice.findAll()).flatMap(type->metadataElementTypesOf(type));
	}
	
	public Stream<MetadataElementType> metadataElementTypesThat(Predicate<MetadataElementType> predicate) {
		return metadataElementTypes().filter(predicate);
	}
	
	public Stream<MetadataElementType> metadataElementTypesOf(MetadataType type) {
		
		return type.getElementTypes().isEmpty() ?   
					metadataElementTypesOf(aMetadataTypeThat(t->t.getCode().equals(type.getCode()))) //from db
				:   shuffle(type.getElementTypes()); //from input
	}
	
	// one
	
	public MetadataElementType aMetadataElementType() {
		return oneof(metadataElementTypes());
	}
	
	public MetadataElementType aMetadataElementTypeThat(Predicate<MetadataElementType> predicate) {
		return oneof(metadataElementTypesThat(predicate));
	}
	
	public MetadataElementType aMetadataElementTypeOf(MetadataType type) {
		return oneof(metadataElementTypesOf(type));
	}
	
	
	//////////////// languages /////////////////////////////////////////////////////////////////////////

	
	// many
	
	public Stream<Language> languages() {
		return shuffle(languageservice.findAll());
	}
	
	public Stream<Language> languagesThat(Predicate<Language> predicate) {
		return languages().filter(predicate);
	}
	
	// one
	
	public Language aLanguage() {
		return oneof(languages());
	}
	
	public Language aLanguageThat(Predicate<Language> predicate) {
		return oneof(languagesThat(predicate));
	}
}
