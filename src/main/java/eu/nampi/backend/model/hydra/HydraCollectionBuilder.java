package eu.nampi.backend.model.hydra;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.sparql.expr.Expr;
import org.apache.jena.sparql.lang.sparql_11.ParseException;
import org.apache.jena.vocabulary.RDF;

import eu.nampi.backend.model.QueryParameters;
import eu.nampi.backend.vocabulary.Core;
import eu.nampi.backend.vocabulary.Doc;
import eu.nampi.backend.vocabulary.Hydra;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HydraCollectionBuilder extends AbstractHydraBuilder {

  public static final Node VAR_FIRST = NodeFactory.createVariable("first");
  public static final Node VAR_LAST = NodeFactory.createVariable("last");
  public static final Node VAR_MANAGES = NodeFactory.createVariable("manages");
  public static final Node VAR_NEXT = NodeFactory.createVariable("next");
  public static final Node VAR_PREVIOUS = NodeFactory.createVariable("previous");
  public static final Node VAR_SEARCH = NodeFactory.createVariable("search");
  public static final Node VAR_TOTAL_ITEMS = NodeFactory.createVariable("totalItems");

  public final ParameterMapper mapper;
  public final WhereBuilder countWhere;
  public final WhereBuilder dataWhere;

  private final QueryParameters params;
  private final SelectBuilder bindSelect = new SelectBuilder();
  private final WhereBuilder bindWhere = new WhereBuilder();

  public HydraCollectionBuilder(String baseUri, Property mainType, QueryParameters params) {
    this(baseUri, mainType, params, true);
  }

  public HydraCollectionBuilder(String baseUri, Property mainType, QueryParameters params, boolean includeTextFilter) {
    super(NodeFactory.createURI(baseUri), mainType);
    this.countWhere = mainWhere();
    this.dataWhere = mainWhere();
    this.mapper = new ParameterMapper(baseUri, VAR_SEARCH, this, bindSelect);
    this.params = params;

    // Add text filter
    dataWhere.addWhere(labelWhere());
    if (includeTextFilter && params.getText().isPresent()) {
      Expr regex = ef.regex(VAR_MAIN_LABEL, params.getText().get(), "i");
      dataWhere.addFilter(regex);
      countWhere.addWhere(labelWhere()).addFilter(regex);
    }

    // Add type filter
    if (params.getType().isPresent()) {
      Resource typeResource = ResourceFactory.createResource(params.getType().get());
      dataWhere.addWhere(VAR_MAIN, RDF.type, typeResource);
      countWhere.addWhere(VAR_MAIN, RDF.type, typeResource);
    }
  }

  @Override
  public String buildHydra() {
    // @formatter:off
    try {
      
      // Construct the result
      this
        // Add general hydra data
        .addConstruct(baseNode, RDF.type, Hydra.Collection)
        .addConstruct(baseNode, Hydra.totalItems, VAR_TOTAL_ITEMS)
        .addConstruct(baseNode, Hydra.manages, VAR_MANAGES)
        .addConstruct(VAR_MANAGES, Hydra.object, Core.event)
        // Add search
        .addConstruct(baseNode, Hydra.search, VAR_SEARCH )
        .addConstruct(VAR_SEARCH, RDF.type, Hydra.IriTemplate)
        .addConstruct(VAR_SEARCH, Hydra.variableRepresentation, Hydra.BasicRepresentation)
        // Add event data
        .addConstruct(baseNode, Hydra.member, VAR_MAIN);

      // Add all variable bindings
      bindWhere
        .addBind(ef.bnode(), VAR_SEARCH)
        .addBind(ef.bnode(), VAR_MANAGES);

      // Set up selects
      SelectBuilder dataSelect = new SelectBuilder()
        .addVar("*")
        .addWhere(dataWhere);
        params.getOrderByClauses().appendAllTo(dataSelect);
        dataSelect.addOrderBy(VAR_MAIN)
        .setOffset(params.getOffset())
        .setLimit(params.getLimit());
      SelectBuilder countSelect = new SelectBuilder()
        .addVar("count(*)", VAR_TOTAL_ITEMS)
        .addWhere(countWhere);
      SelectBuilder contentSelect = new SelectBuilder()
        .addVar("*")
        .addUnion(dataSelect)
        .addUnion(countSelect);
      bindSelect 
        .addVar("*")
        .addWhere(bindWhere);

      Node view = mapper
        .add("limit", Hydra.limit, params.getLimit())
        .add("offset", Hydra.offset, params.getOffset())
        .add("orderBy", Doc.eventOrderByVar, params.getOrderByClauses().toQueryString())
        .add("pageIndex", Hydra.pageIndex, null)
        .add("text", Doc.textVar, params.getText().orElse(""))
        .add("type", RDF.type, params.getType().orElse(""))
        .addTemplate(baseNode);

      this
        .addConstruct(view, Hydra.first, VAR_FIRST)
        .addConstruct(view, Hydra.previous, VAR_PREVIOUS)
        .addConstruct(view, Hydra.next, VAR_NEXT)
        .addConstruct(view, Hydra.last, VAR_LAST)
        .addWhere(new WhereBuilder()
        .addUnion(contentSelect)
        .addUnion(bindSelect))
        .addBind("if(contains('" + view + "', 'offset=0'), 1+'', replace('" + view + "', 'offset=\\\\d*', 'offset=0'))", VAR_FIRST)
        .addBind("if(" + params.getOffset() + " >= floor(" + VAR_TOTAL_ITEMS + " / " + params.getLimit() + ") * " + params.getLimit() + " , 1+'', replace('" + view + "', 'offset=\\\\d*', concat('offset=', str(xsd:integer(floor(" + VAR_TOTAL_ITEMS + " / " + params.getLimit() + ") * " + params.getLimit() + ")))))", VAR_LAST)
        .addBind("if(" + (params.getOffset() - params.getLimit()) + " >= 0, iri(replace('" + view + "', 'offset=\\\\d*', concat('offset=', str(" + (params.getOffset() - params.getLimit()) + ")))), 1+'')", VAR_PREVIOUS)
        .addBind("if(" + (params.getOffset() + params.getLimit()) + " < " + VAR_TOTAL_ITEMS + ", replace('" + view + "', 'offset=\\\\d*', concat('offset=', str(" + (params.getOffset() + params.getLimit()) + "))), 1+'')", VAR_NEXT);
    } catch (ParseException e) {
      log.error(e.getMessage());
    }
      // @formatter:on
    return buildString();
  }
}