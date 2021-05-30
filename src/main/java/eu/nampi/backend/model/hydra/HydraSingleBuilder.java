package eu.nampi.backend.model.hydra;

import java.util.function.BiFunction;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import eu.nampi.backend.service.JenaService;

public class HydraSingleBuilder extends AbstractHydraBuilder {

  public HydraSingleBuilder(JenaService jenaService, String baseUri, Resource mainType) {
    super(jenaService, baseUri, mainType);
    coreData
        .addFilter(ef.sameTerm(VAR_MAIN, root))
        .addWhere(VAR_MAIN, RDF.type, VAR_TYPE)
        .addFilter(ef.not(ef.strstarts(ef.str(VAR_TYPE), OWL.getURI())))
        .addFilter(ef.not(ef.strstarts(ef.str(VAR_TYPE), RDFS.getURI())))
        .addFilter(ef.not(ef.strstarts(ef.str(VAR_TYPE), RDF.getURI())))
        .addOptional(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addOptional(VAR_MAIN, RDFS.comment, VAR_COMMENT);
  }

  @Override
  public void build(BiFunction<Model, QuerySolution, RDFNode> rowToNode) {
    SelectBuilder core = new SelectBuilder()
        .addVar("*")
        .addWhere(coreData);
    jenaService.select(core, row -> rowToNode.apply(this.model, row));
  }

}
