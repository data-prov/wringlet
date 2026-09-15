package org.dataprov.dp.wringlet

import org.apache.spark.sql.SparkSession
import org.dataprov.dp.wringlet.ProvenanceApi._
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlan
import org.apache.spark.sql.catalyst.plans.logical.LogicalPlanIntegrity
import org.apache.spark.sql.catalyst.plans.logical.Project
import org.apache.spark.sql.catalyst.plans.logical.Filter
import org.apache.spark.sql.catalyst.plans.logical.Sort
import org.apache.spark.sql.catalyst.plans.logical.Join
import org.apache.spark.sql.catalyst.plans.logical.Intersect
import org.apache.spark.sql.catalyst.plans.logical.Aggregate
import org.apache.spark.sql.catalyst.plans.logical.Union
import org.apache.spark.sql.catalyst.plans.logical.Distinct
import org.apache.spark.sql.catalyst.plans.logical.Deduplicate
import org.apache.spark.sql.catalyst.plans.logical.Window
import org.apache.spark.sql.catalyst.plans.logical.Except
import org.apache.spark.sql.catalyst.expressions.Attribute
import org.apache.spark.sql.catalyst.expressions.Alias
import org.apache.spark.sql.catalyst.expressions.InSubquery


private[wringlet] final class ProvenancePlanRewriter(
    spark: SparkSession,
) {
  def applyLocally: PartialFunction[LogicalPlan, LogicalPlan] = {
    case project: Project => rewriteProject(project)
    case filter: Filter => rewriteFilter(filter)
    case sort: Sort => rewriteSort(sort)
    case join: Join => rewriteJoin(join)
    case intersect: Intersect => rewriteIntersect(intersect)
    case aggregate: Aggregate => rewriteAggregate(aggregate)
    case union: Union => rewriteUnion(union)
    case distinct: Distinct => rewriteDistinct(distinct)
    case deduplicate: Deduplicate => rewriteDeduplicate(deduplicate)
    case window: Window => rewriteWindow(window)
    case except: Except => rewriteExcept(except)
  }

  def provColName: String = provenanceColumnName(spark)

  def outputsProvenance(plan: LogicalPlan): Boolean = {
    // A plan outputs provenance if it has the provenance column in its output attributes.
    LogicalPlanIntegrity.canGetOutputAttrs(plan) && plan.output.exists(
    _.name == provColName
    )
  }
    
  def getProvenanceAttribute(plan: LogicalPlan): Attribute = {
    // Retrieves the provenance attribute from the plan's output attributes.
    if (LogicalPlanIntegrity.canGetOutputAttrs(plan)){
        plan.output.find(_.name == provColName).get
    } else throw new IllegalArgumentException("Plan is not resolved")
  }
    

  /**
    * Rewrites a Project node to ensure that the provenance column is included if the child outputs it.
    * Project nodes correspond to SELECT statements in SQL and select/withColumn operations in Spark.
    * They define which columns are included in the output.
    *
    * @param project The Project node to rewrite.
    * @return The rewritten Project node with the provenance column included if necessary.
    */
  private def rewriteProject(project: Project): LogicalPlan = {
    val projectListsProvenance = project.projectList.exists(_.name == provColName)

    if (!projectListsProvenance && outputsProvenance(project.child)) {
        // If the project node does not list the provenance column, but the child outputs it:
        // we need to add the provenance column to the project list.
        val provenance = getProvenanceAttribute(project.child)
        project.copy(
            projectList = project.projectList :+ Alias(provenance, provColName)()
        )
    } else {
        // If the project node already lists the provenance column or the child does not output it anyway:
        // we do not need to modify the project node.
        project
    }
  }

  private def rewriteFilter(filter: Filter): LogicalPlan = {
    // TODO: wrong logic! => there is probably something to be done with ProvenancePredicateSubqueryRule
    // Filter nodes correspond to WHERE clauses in SQL and filter operations in Spark.
    // They do not change the output columns, so we can simply rewrite the child and keep the filter as is.
    val newCondition = filter.condition.transform {
        // We look for 'InSubquery' expressions, which represent IN subqueries
        // Provenance should not be included in the IN subquery's output, so we remove it if present.
        case inSub @ InSubquery(values, query) =>
            val subPlan = query.plan
            // If the IN subquery plan contains the provenance column, we need to remove it
            if (outputsProvenance(subPlan)) {
                val outputsWithoutProvenance = subPlan.output.filter(_.name != provColName)
                val subqueryPlanWithoutProvenance = Project(outputsWithoutProvenance, subPlan)
                // We create a new ListQuery with the provenance column removed
                val newQuery = query.copy(
                    plan = subqueryPlanWithoutProvenance,
                    numCols = outputsWithoutProvenance.length
                )
                inSub.copy(query = newQuery)
            } else {
                inSub
            }
    }
    // We return a new Filter node with the updated condition
    filter.copy(condition = newCondition)
    filter
  }

  private def rewriteSort(sort: Sort): LogicalPlan = ???
  private def rewriteJoin(join: Join): LogicalPlan = ???
  private def rewriteIntersect(intersect: Intersect): LogicalPlan = ???
  private def rewriteAggregate(aggregate: Aggregate): LogicalPlan = ???
  private def rewriteUnion(union: Union): LogicalPlan = ???
  private def rewriteDistinct(distinct: Distinct): LogicalPlan = ???
  private def rewriteDeduplicate(deduplicate: Deduplicate): LogicalPlan = ???
  private def rewriteWindow(window: Window): LogicalPlan = ???
  private def rewriteExcept(except: Except): LogicalPlan = ???
}
