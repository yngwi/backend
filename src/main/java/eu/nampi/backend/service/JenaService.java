package eu.nampi.backend.service;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.rdf.model.Model;

import eu.nampi.backend.model.HydraBuilder;

public interface JenaService {

  public Model construct(ConstructBuilder constructBuilder);

  public Model construct(HydraBuilder hydraBuilder);

  public void initInfCache();

}
