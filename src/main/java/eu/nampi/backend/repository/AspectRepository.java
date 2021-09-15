package eu.nampi.backend.repository;

import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_COMMENT;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_LABEL;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_MAIN;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_TYPE;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiFunction;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.arq.querybuilder.WhereBuilder;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import eu.nampi.backend.model.QueryParameters;
import eu.nampi.backend.model.hydra.AbstractHydraBuilder;
import eu.nampi.backend.model.hydra.HydraCollectionBuilder;
import eu.nampi.backend.model.hydra.HydraSingleBuilder;
import eu.nampi.backend.utils.HydraUtils;
import eu.nampi.backend.vocabulary.Api;
import eu.nampi.backend.vocabulary.Core;

@Repository
@CacheConfig(cacheNames = "aspects")
public class AspectRepository extends AbstractHydraRepository {

  private static final String ENDPOINT_NAME = "aspects";
  private static final Node VAR_SAME_AS = NodeFactory.createVariable("sameAs");
  private static final Node VAR_STRING = NodeFactory.createVariable("string");

  @Autowired
  HierarchyRepository hierarchyRepository;

  private static final BiFunction<Model, QuerySolution, RDFNode> ROW_MAPPER = (model, row) -> {
    Resource main = row.getResource(VAR_MAIN.toString());
    // Main
    Optional.ofNullable(row.getResource(VAR_TYPE.toString())).ifPresentOrElse(type -> {
      model.add(main, RDF.type, type);
    }, () -> {
      model.add(main, RDF.type, Core.aspect);
    });
    // Label
    Optional
        .ofNullable(row.getLiteral(VAR_LABEL.toString()))
        .ifPresent(label -> model.add(main, RDFS.label, label));
    // Comment
    Optional
        .ofNullable(row.getLiteral(VAR_COMMENT.toString()))
        .ifPresent(comment -> model.add(main, RDFS.comment, comment));
    // Text
    Optional
        .ofNullable(row.getLiteral(VAR_STRING.toString()))
        .ifPresent(text -> model.add(main, Core.hasText, text));
    // SameAs
    Optional
        .ofNullable(row.getResource(VAR_SAME_AS.toString()))
        .ifPresent(iri -> model.add(main, Core.sameAs, iri));
    return main;
  };

  @Cacheable(
      key = "{#lang, #params.limit, #params.offset, #params.orderByClauses, #params.type, #params.text, #participant}")
  public String findAll(QueryParameters params, Lang lang, Optional<Resource> participant) {
    HydraCollectionBuilder builder = new HydraCollectionBuilder(jenaService,
        endpointUri(ENDPOINT_NAME), Core.aspect, Api.aspectOrderByVar, params, false);
    ExprFactory ef = builder.ef;

    // Add participant query
    builder.mapper.add("participant", Api.aspectParticipantVar, participant);
    participant.ifPresent(resParticipant -> {
      Path path = PathFactory.pathSeq(PathFactory.pathLink(Core.aspectIsUsedIn.asNode()),
          PathFactory.pathLink(Core.hasParticipant.asNode()));
      builder.coreData.addWhere(VAR_MAIN, path, resParticipant);
    });

    // Add custom text select
    params.getText().ifPresent(text -> {
      Node varSearchString = NodeFactory.createVariable("searchString");
      Path path = PathFactory.pathAlt(PathFactory.pathLink(RDFS.label.asNode()),
          PathFactory.pathLink(Core.hasText.asNode()));
      builder.coreData.addOptional(VAR_MAIN, path, varSearchString)
          .addFilter(ef.regex(varSearchString, params.getText().get(), "i"));
    });

    addData(builder.extendedData);
    return build(builder, lang);
  }

  @Cacheable(key = "{#lang, #id}")
  public String findOne(Lang lang, UUID id) {
    HydraSingleBuilder builder =
        new HydraSingleBuilder(jenaService, endpointUri(ENDPOINT_NAME, id.toString()), Core.aspect);
    addData(builder.coreData);
    return build(builder, lang);
  }

  private String build(AbstractHydraBuilder builder, Lang lang) {
    builder.build(ROW_MAPPER);
    return HydraUtils.serialize(builder.model, lang, builder.root);
  }

  private void addData(WhereBuilder builder) {
    builder
        .addOptional(VAR_MAIN, Core.hasText, VAR_STRING)
        .addOptional(VAR_MAIN, Core.sameAs, VAR_SAME_AS);
  }

  public String insert(Lang lang, Resource type, List<Literal> labels, List<Literal> comments,
      List<Literal> texts) {
    UUID id = UUID.randomUUID();
    Resource aspect = ResourceFactory.createResource(endpointUri(ENDPOINT_NAME, id.toString()));
    if (!hierarchyRepository.isSubtype(Core.aspect, type)) {
      throw new IllegalArgumentException(
          String.format("'%s' is not a subtype of '%s'.", type.toString(), Core.aspect.toString()));
    }
    UpdateBuilder builder = new UpdateBuilder()
        .addInsert(aspect, RDF.type, type);
    labels.forEach(label -> builder.addInsert(aspect, RDFS.label, label));
    comments.forEach(labelcomment -> builder.addInsert(aspect, RDFS.comment, labelcomment));
    texts.forEach(text -> builder.addInsert(aspect, Core.hasText, text));
    jenaService.update(builder);
    return findOne(lang, id);
  }

  public String update(Lang lang, UUID id, Resource type, List<Literal> labels,
      List<Literal> comments, List<Literal> texts) {
    Resource aspect = ResourceFactory.createResource(endpointUri(ENDPOINT_NAME, id));
    if (!hierarchyRepository.isSubtype(Core.aspect, type)) {
      throw new IllegalArgumentException(
          String.format("'%s' is not a subtype of '%s'.", type.toString(), Core.aspect.toString()));
    }
    Node varText = NodeFactory.createVariable("text");
    UpdateBuilder builder = new UpdateBuilder();
    ExprFactory ef = builder.getExprFactory();
    builder
        .addDelete(VAR_MAIN, RDF.type, VAR_TYPE)
        .addDelete(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addDelete(VAR_MAIN, RDFS.comment, VAR_COMMENT)
        .addDelete(VAR_MAIN, Core.hasText, varText)
        .addInsert(VAR_MAIN, RDF.type, type)
        .addFilter(ef.sameTerm(VAR_MAIN, aspect))
        .addWhere(VAR_MAIN, RDF.type, VAR_TYPE)
        .addWhere(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addOptional(VAR_MAIN, RDFS.comment, VAR_COMMENT)
        .addOptional(VAR_MAIN, Core.hasText, varText);
    labels.forEach(label -> builder.addInsert(VAR_MAIN, RDFS.label, label));
    comments.forEach(labelcomment -> builder.addInsert(aspect, RDFS.comment, labelcomment));
    texts.forEach(text -> builder.addInsert(aspect, Core.hasText, text));
    jenaService.update(builder);
    return findOne(lang, id);
  }
}
