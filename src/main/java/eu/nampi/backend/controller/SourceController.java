package eu.nampi.backend.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import eu.nampi.backend.model.InsertResult;
import eu.nampi.backend.model.OrderByClauses;
import eu.nampi.backend.model.QueryParameters;
import eu.nampi.backend.repository.SourceRepository;

@RestController
public class SourceController extends AbstractRdfController {

  @Autowired
  SourceRepository sourceRepository;

  @GetMapping(value = "/sources", produces = {"application/ld+json", "text/turtle",
      "application/rdf+xml", "application/n-triples"})
  public ResponseEntity<String> getSources(
      @RequestHeader("accept") Lang lang,
      @RequestParam("page") Optional<Integer> page,
      @RequestParam("pageIndex") Optional<Integer> pageIndex,
      @RequestParam("limit") Optional<Integer> limit,
      @RequestParam("offset") Optional<Integer> offset,
      @RequestParam("orderBy") Optional<OrderByClauses> orderBy,
      @RequestParam("type") Optional<Resource> type,
      @RequestParam("text") Optional<Literal> text) {
    QueryParameters params = getParameters(page, pageIndex, limit, offset, orderBy, type, text);
    String result = sourceRepository.findAll(params, lang);
    return new ResponseEntity<String>(result, HttpStatus.OK);
  }

  @GetMapping(value = "/sources/{id}", produces = {"application/ld+json", "text/turtle",
      "application/rdf+xml", "application/n-triples"})
  public ResponseEntity<String> getEvent(
      @RequestHeader("accept") Lang lang,
      @PathVariable UUID id) {
    String result = sourceRepository.findOne(lang, id);
    return new ResponseEntity<String>(result, HttpStatus.OK);
  }

  @PostMapping(value = "/sources", produces = {"application/ld+json", "text/turtle",
      "application/rdf+xml", "application/n-triples"})
  public ResponseEntity<String> postSource(
      @RequestHeader("accept") Lang lang,
      @RequestParam("type") Resource type,
      @RequestParam("label[]") List<Literal> label,
      @RequestParam(value = "comment[]", required = false) List<Literal> comment,
      @RequestParam(value = "text[]", required = false) List<Literal> text,
      @RequestParam(value = "sameAs[]", required = false) List<Resource> sameAs) {
    InsertResult result =
        sourceRepository.insert(lang, type, label, asList(comment), asList(text), asList(sameAs));
    HttpHeaders headers = new HttpHeaders();
    headers.add("Location", result.getEntity().getURI());
    return new ResponseEntity<String>(result.getResponseBody(), headers, HttpStatus.CREATED);
  }

  @PutMapping(value = "/sources/{id}", produces = {"application/ld+json", "text/turtle",
      "application/rdf+xml", "application/n-triples"})
  public ResponseEntity<String> putSource(
      @RequestHeader("accept") Lang lang,
      @PathVariable UUID id,
      @RequestParam("type") Resource type,
      @RequestParam("label[]") List<Literal> label,
      @RequestParam(value = "comment[]", required = false) List<Literal> comment,
      @RequestParam(value = "text[]", required = false) List<Literal> text,
      @RequestParam(value = "sameAs[]", required = false) List<Resource> sameAs) {
    String newSource = sourceRepository.update(lang, id, type, label, asList(comment), asList(text),
        asList(sameAs));
    return new ResponseEntity<String>(newSource, HttpStatus.OK);
  }

  @DeleteMapping(value = "/sources/{id}")
  public ResponseEntity<?> deleteSource(@PathVariable UUID id) {
    sourceRepository.delete(id);
    return new ResponseEntity<>(HttpStatus.OK);
  }
}
