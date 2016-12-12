package net.sourceforge.ondex.plugins.test_plugin;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.opencsv.CSVReader;

import net.sourceforge.ondex.args.ArgumentDefinition;
import net.sourceforge.ondex.args.FileArgumentDefinition;
import net.sourceforge.ondex.core.ConceptClass;
import net.sourceforge.ondex.core.DataSource;
import net.sourceforge.ondex.core.EntityFactory;
import net.sourceforge.ondex.core.EvidenceType;
import net.sourceforge.ondex.core.MetaData;
import net.sourceforge.ondex.core.ONDEXConcept;
import net.sourceforge.ondex.core.ONDEXEntity;
import net.sourceforge.ondex.core.ONDEXRelation;
import net.sourceforge.ondex.core.RelationType;
import net.sourceforge.ondex.parser.ONDEXParser;

/**
 * TODO: comment me!
 *
 * @author brandizi
 * <dl><dt>Date:</dt><dd>30 Nov 2016</dd></dl>
 *
 */
public class TestParser extends ONDEXParser
{
  private Logger log = Logger.getLogger ( this.getClass() );

	public String getId ()
	{
		return "testParser";
	}

	public String getName ()
	{
		return "Foo Test Parser";
	}

	public String getVersion ()
	{
		return "1.0-SNAPSHOT";
	}

	public ArgumentDefinition<?>[] getArgumentDefinitions ()
	{
		return new ArgumentDefinition<?>[] {
      new FileArgumentDefinition ( 
      	FileArgumentDefinition.INPUT_FILE, FileArgumentDefinition.INPUT_FILE_DESC, true, true, false, false
      )
		};
	}

	public void start () throws Exception
	{		
		ConceptClass ccProtein = requireConceptClass ( "Protein" );
		ConceptClass ccPub = requireConceptClass ( "Publication" );
		
		// ONDEX doesn't seem to recognise identity
		Table<String, MetaData, ONDEXEntity> visitedEntities = HashBasedTable.create ();
		
		DataSource dsUnknown = requireDataSource ( "unknown" );
		EvidenceType etImpd = requireEvidenceType ( "IMPD" );
		
		RelationType rtInteractsWith = requireRelationType ( "it_wi" );

    File inFile = new File ( (String) getArguments().getUniqueValue( FileArgumentDefinition.INPUT_FILE ) );
		
		CSVReader csvin = new CSVReader ( new BufferedReader ( new FileReader ( inFile ) ), '\t', '"'	);
		Map<String,Integer> headers = new HashMap<String,Integer> ();
		for ( String[] line ; ( line = csvin.readNext () ) != null ; )
		{
			if ( headers.isEmpty () ) {
				for ( int icol = 0; icol < line.length; icol++ ) headers.put ( line [ icol ], icol );
				continue;
			}
			
			String uniProtId1 = line [ headers.get ( "ID(s) interactor A" ) ];
			String uniProtId2 = line [ headers.get ( "ID(s) interactor B" ) ];

			if ( !uniProtId1.startsWith ( "uniprotkb:" ) || !uniProtId2.startsWith ( "uniprotkb:" ) )
				continue;
			
			EntityFactory entFact = graph.getFactory();
			
			uniProtId1 = uniProtId1.substring ( "uniprotkb:".length () );
      ONDEXConcept cProt1 = (ONDEXConcept) visitedEntities.get ( uniProtId1, ccProtein );
      if ( cProt1 == null ) visitedEntities.put ( 
      	uniProtId1, ccProtein, cProt1 = entFact.createConcept ( uniProtId1, dsUnknown, ccProtein, etImpd )
      );

			uniProtId2 = uniProtId2.substring ( "uniprotkb:".length () );
      ONDEXConcept cProt2 = (ONDEXConcept) visitedEntities.get ( uniProtId2, ccProtein );
      if ( cProt2 == null ) visitedEntities.put ( 
    		uniProtId2, ccProtein, cProt2 = entFact.createConcept ( uniProtId2, dsUnknown, ccProtein, etImpd )
      );
      
      String relId = uniProtId1 + ":" + uniProtId1;
      ONDEXRelation rel = (ONDEXRelation) visitedEntities.get ( relId, rtInteractsWith );
      if ( rel == null ) visitedEntities.put (
      	relId, rtInteractsWith, rel = entFact.createRelation ( cProt1, cProt2, rtInteractsWith, etImpd )
      );
    
      log.info ( String.format ( "Interaction: %s %s", uniProtId1, uniProtId2 ) );
      
		} // for()
		
		csvin.close ();
	}

	public String[] requiresValidators ()
	{
		return new String [ 0 ];
	}

}
