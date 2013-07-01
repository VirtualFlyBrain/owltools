package owltools.sim2;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Set;

import org.apache.commons.math.MathException;
import org.apache.log4j.Logger;
import org.junit.Test;
import org.obolibrary.oboformat.parser.OBOFormatParserException;
import org.semanticweb.elk.owlapi.ElkReasonerFactory;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

import owltools.OWLToolsTestBasics;
import owltools.graph.OWLGraphWrapper;
import owltools.io.OWLPrettyPrinter;
import owltools.io.ParserWrapper;
import owltools.io.TableToAxiomConverter;
import owltools.sim2.SimpleOwlSim.ScoreAttributesPair;
import owltools.sim2.preprocessor.NullSimPreProcessor;
import owltools.sim2.preprocessor.SimPreProcessor;

/**
 * This is the main test class for PropertyViewOntologyBuilder
 * 
 * @author cjm
 *
 */
public class BasicOWLSimTest extends OWLToolsTestBasics {

	private Logger LOG = Logger.getLogger(BasicOWLSimTest.class);
	OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
	OWLDataFactory df = manager.getOWLDataFactory();
	OWLOntology sourceOntol;
	SimPreProcessor pproc;
	OWLPrettyPrinter owlpp;
	OWLGraphWrapper g;
	SimpleOwlSim sos;

	@Test
	public void testBasicSim() throws IOException, OWLOntologyCreationException, OWLOntologyStorageException, MathException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOWL(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			sos = new SimpleOwlSim(sourceOntol);
			sos.setReasoner(reasoner);
			LOG.info("Reasoner="+sos.getReasoner());

			//sos.saveOntology("/tmp/z.owl");

			reasoner.flush();
			for (OWLNamedIndividual i : sourceOntol.getIndividualsInSignature()) {
				//System.out.println("COMPARING: "+i);
				for (OWLNamedIndividual j : sourceOntol.getIndividualsInSignature()) {
					showSim(i,j);
				}
			}
		}
		finally {
			reasoner.dispose();
		}
	}

	@Test 
	public void testGetEntropy() throws OWLOntologyCreationException, IOException, OBOFormatParserException {
		ParserWrapper pw = new ParserWrapper();
		sourceOntol = pw.parseOBO(getResourceIRIString("sim/mp-subset-1.obo"));
		g =  new OWLGraphWrapper(sourceOntol);
		parseAssociations(getResource("sim/mgi-gene2mp-subset-1.tbl"), g);

		owlpp = new OWLPrettyPrinter(g);

		// assume buffering
		OWLReasoner reasoner = new ElkReasonerFactory().createReasoner(sourceOntol);
		try {

			sos = new SimpleOwlSim(sourceOntol);
			sos.setReasoner(reasoner);

			reasoner.flush();
			Double e = sos.getEntropy();
			LOG.info("ENTROPY OF ONTOLOGY = "+e);

			for (String subset : g.getAllUsedSubsets()) {
				LOG.info("SUBSET:"+subset);
				e = sos.getEntropy(g.getOWLClassesInSubset(subset));
				LOG.info(" ENTROPY OF "+subset+" = "+e);
			}
		}
		finally {
			reasoner.dispose();
		}		
	}

	protected void parseAssociations(File file, OWLGraphWrapper g) throws IOException {
		TableToAxiomConverter ttac = new TableToAxiomConverter(g);
		ttac.config.axiomType = AxiomType.CLASS_ASSERTION;
		ttac.config.isSwitchSubjectObject = true;
		ttac.parse(file);			
	}



	private void showSim(OWLNamedIndividual i, OWLNamedIndividual j) {

		if (i==j) return;
		float s = sos.getElementJaccardSimilarity(i, j);
		if (s > 0.1) {
			LOG.info("SimJ( "+i+" , "+j+" ) = "+s);

			ScoreAttributesPair maxic = sos.getSimilarityMaxIC(i, j);
			LOG.info("MaxIC( "+i+" , "+j+" ) = "+maxic.score+" "+show(maxic.attributeClassSet));

			ScoreAttributesPair bma = sos.getSimilarityBestMatchAverageAsym(i, j);
			LOG.info("BMAasym( "+i+" , "+j+" ) = "+bma.score+" "+show(bma.attributeClassSet));
		}

	}




	private String show(Set<OWLClassExpression> cset) {
		StringBuffer sb = new StringBuffer();
		for (OWLClassExpression c : cset) {
			sb.append(owlpp.render(c) + " ; ");
		}
		return sb.toString();
	}

	private OWLClass get(String iri) {
		return df.getOWLClass(IRI.create("http://x.org#"+iri));
	}





}
