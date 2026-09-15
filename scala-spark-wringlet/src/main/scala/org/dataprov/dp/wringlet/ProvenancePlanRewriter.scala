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

  def outputsProvenance(plan: LogicalPlan): Boolean =
    LogicalPlanIntegrity.canGetOutputAttrs(plan) && plan.output.exists(
      _.name == provColName
    )
  
  def getProvenanceAttribute(plan: LogicalPlan): Attribute =
    if (LogicalPlanIntegrity.canGetOutputAttrs(plan))
      plan.output.reverse.find(_.name == provColName).get
    else throw new IllegalArgumentException("Plan is not resolved")

  private def rewriteProject(project: Project): LogicalPlan = {
    val projectHasProvenance = project.projectList.exists(_.name == provColName)

    if (!projectHasProvenance && outputsProvenance(project.child)) {
        val provenance = getProvenanceAttribute(project.child)
        project.copy(
            projectList = project.projectList :+ Alias(provenance, provColName)()
        )
    } else {
        project
    }
  }

//     val safeProjectList = project.projectList.asInstanceOf[Seq[Expression]]
    
//     // We check if the child has the provenance column and if the project itself already has it
//     val childHasProv = hasProv(project.child, provenanceColName)

//     // We partition the projectList into provenance expressions and non-provenance expressions
//     val (provExprs, nonProvExprs) = safeProjectList.partition {
//         case Alias(_, name)  => name == provenanceColName
//         case attr: Attribute => attr.name == provenanceColName
//         case _               => false
//     }

//     // We filter the non-provenance expressions to keep only those that reference
//     // columns from the child output
//     val validNonProvExprs = nonProvExprs.filter(expr =>
//         expr.references.subsetOf(project.child.outputSet)
//     )

//     if (childHasProv) {
//         val childProvAttr = getProvAttr(child, provenanceColName)
//         // Reuse the existing provenance expression if all its references are still
//         // satisfied by the current child output (multi-pass stability: a fresh alias
//         // added on a previous pass is preserved unchanged on subsequent passes).
//         // Otherwise create a fresh Alias so that two derivations of the same source
//         // each get a distinct ExprId — required for correct self-join provenance
//         // tracking (without this, both sides of the join share the same ExprId and
//         // Spark resolves both references to the same row value).
//         val provExpr = provExprs
//         .collectFirst {
//             case expr if expr.references.subsetOf(child.outputSet) => expr
//         }
//         .getOrElse(Alias(childProvAttr, provenanceColName)())
//         project.copy(
//         projectList = (validNonProvExprs :+ provExpr)
//             .asInstanceOf[Seq[NamedExpression]],
//         child = child
//         )
//     } else if (validNonProvExprs.size != nonProvExprs.size) {
//         // If some expressions were removed because they reference columns that are no longer present
//         // in the child output, we need to update the project list
//         project.copy(
//         projectList =
//             validNonProvExprs.asInstanceOf[Seq[NamedExpression]],
//         child = child
//         )
//     } else {
//         project
//     }
//   }
  private def rewriteFilter(filter: Filter): LogicalPlan = ???
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
