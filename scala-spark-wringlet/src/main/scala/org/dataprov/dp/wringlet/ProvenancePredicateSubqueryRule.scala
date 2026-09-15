package org.dataprov.dp.wringlet

import org.apache.spark.sql.SparkSession
import org.apache.spark.sql.catalyst.optimizer.RewritePredicateSubquery
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.rules.Rule

private[wringlet] final case class ProvenancePredicateSubqueryRule(
    spark: SparkSession,
    provenanceBuilder: ProvenanceBuilder
) extends Rule[LogicalPlan] {

  override def apply(plan: LogicalPlan): LogicalPlan = {
    val predicateSubqueriesRewritten = RewritePredicateSubquery(plan)
    LogicalPlanWithProvenance(spark, provenanceBuilder)(predicateSubqueriesRewritten)
  }
}