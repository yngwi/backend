package eu.nampi.backend.repository;

import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_COMMENT;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_LABEL;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_MAIN;
import static eu.nampi.backend.model.hydra.AbstractHydraBuilder.VAR_TYPE;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import org.apache.jena.arq.querybuilder.ExprFactory;
import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;
import eu.nampi.backend.model.Author;
import eu.nampi.backend.model.QueryParameters;
import eu.nampi.backend.model.hydra.AbstractHydraBuilder;
import eu.nampi.backend.model.hydra.HydraCollectionBuilder;
import eu.nampi.backend.model.hydra.HydraSingleBuilder;
import eu.nampi.backend.utils.HydraUtils;
import eu.nampi.backend.vocabulary.Api;
import eu.nampi.backend.vocabulary.Core;

@Repository
@CacheConfig(cacheNames = "authors")
public class AuthorRepository extends AbstractHydraRepository {

  private static final String ENDPOINT_NAME = "authors";

  private static final BiFunction<Model, QuerySolution, RDFNode> ROW_MAPPER = (model, row) -> {
    Resource main = row.getResource(VAR_MAIN.toString());
    // Main
    Optional
        .ofNullable(row.getResource(VAR_TYPE.toString()))
        .ifPresentOrElse(type -> {
          model.add(main, RDF.type, type);
        }, () -> {
          model.add(main, RDF.type, Core.author);
        });
    // Label
    Optional
        .ofNullable(row.getLiteral(VAR_LABEL.toString()))
        .ifPresent(label -> model.add(main, RDFS.label, label));
    // Comment
    Optional
        .ofNullable(row.getLiteral(VAR_COMMENT.toString()))
        .ifPresent(comment -> model.add(main, RDFS.comment, comment));
    return main;
  };

  @Cacheable(
      key = "{#lang, #params.limit, #params.offset, #params.orderByClauses, #params.type, #params.text}")
  public String findAll(QueryParameters params, Lang lang) {
    HydraCollectionBuilder builder =
        new HydraCollectionBuilder(jenaService, endpointUri(ENDPOINT_NAME), Core.author,
            Api.authorOrderByVar, params);
    return build(builder, lang);
  }

  @Cacheable(key = "{#lang, #id}")
  public String findOne(Lang lang, UUID id) {
    HydraSingleBuilder builder =
        new HydraSingleBuilder(jenaService, endpointUri(ENDPOINT_NAME, id.toString()),
            Core.author);
    return build(builder, lang);
  }

  private String build(AbstractHydraBuilder builder, Lang lang) {
    builder.build(ROW_MAPPER);
    return HydraUtils.serialize(builder.model, lang, builder.root);
  }

  public Optional<Author> findOne(UUID rdfId) {
    AtomicReference<Optional<Author>> authorRef = new AtomicReference<>(Optional.empty());
    SelectBuilder builder = new SelectBuilder();
    ExprFactory ef = builder.getExprFactory();
    String authorIri = endpointUri(ENDPOINT_NAME, rdfId);
    Resource author = ResourceFactory.createResource(authorIri);
    builder
        .addValueVar(VAR_LABEL)
        .addWhere(VAR_MAIN, RDF.type, Core.author)
        .addWhere(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addFilter(ef.sameTerm(VAR_MAIN, author));
    jenaService.select(builder, (qs) -> {
      String label = qs.getLiteral(VAR_LABEL.toString()).getString();
      authorRef.set(Optional.of(new Author(authorIri, rdfId, label)));
    });
    return authorRef.get();
  }

  public Author addOne(UUID rdfId, String label) {
    String iri = endpointUri(ENDPOINT_NAME, rdfId);
    Resource authorRes = ResourceFactory.createResource(iri);
    UpdateBuilder updateBuilder = new UpdateBuilder()
        .addInsert(authorRes, RDF.type, Core.author)
        .addInsert(authorRes, RDFS.label, label);
    jenaService.update(updateBuilder);
    return new Author(iri, rdfId, label);
  }

  public Author updateLabel(Author author, String newLabel) {
    UpdateBuilder builder = new UpdateBuilder();
    ExprFactory ef = builder.getExprFactory();
    builder
        .addDelete(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addInsert(VAR_MAIN, RDFS.label, newLabel)
        .addWhere(VAR_MAIN, RDF.type, Core.author)
        .addWhere(VAR_MAIN, RDFS.label, VAR_LABEL)
        .addFilter(ef.sameTerm(VAR_MAIN, author));
    author.setLabel(newLabel);
    jenaService.update(builder);
    return author;
  }
}
