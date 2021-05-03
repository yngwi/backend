package eu.nampi.backend.service;

import java.util.function.Consumer;

import org.apache.jena.arq.querybuilder.SelectBuilder;
import org.apache.jena.arq.querybuilder.UpdateBuilder;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Model;

import eu.nampi.backend.model.hydra.InterfaceHydraBuilder;
import eu.nampi.backend.model.hydra.InterfaceHydraBuilderOld;

public interface JenaService {

  public Model construct(InterfaceHydraBuilder constructBuilder);

  public Model construct(InterfaceHydraBuilderOld hydraBuilder);

  public void initInfCache();

  public void select(SelectBuilder selectBuilder, Consumer<QuerySolution> rowAction);

  public void update(UpdateBuilder updateBuilder);
}
