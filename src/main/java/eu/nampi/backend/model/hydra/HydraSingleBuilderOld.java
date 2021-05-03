package eu.nampi.backend.model.hydra;

import org.apache.jena.query.Query;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.sparql.lang.sparql_11.ParseException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class HydraSingleBuilderOld extends AbstractHydraBuilderOld<HydraSingleBuilderOld> {

  public HydraSingleBuilderOld(String uri, Property mainType) {
    super(mainType);
    try {
      this.mainWhere.addFilter(MAIN_SUBJ + " = <" + uri + ">");
    } catch (ParseException e) {
      log.error(e.getMessage());
    }
  }

  public HydraSingleBuilderOld addData(Object s, Object p, Object o) {
    addConstruct(s, p, o);
    addWhere(s, p, o);
    return getThis();
  }

  public HydraSingleBuilderOld addMainData(Object p, Object o) {
    return addData(MAIN_SUBJ, p, o);
  }

  @Override
  public Query build() {
    return this.builder.addWhere(this.mainWhere).build();
  }

  @Override
  protected HydraSingleBuilderOld getThis() {
    return this;
  }

}
