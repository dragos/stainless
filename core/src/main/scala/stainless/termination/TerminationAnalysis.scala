/* Copyright 2009-2016 EPFL, Lausanne */

package stainless
package termination

trait TerminationAnalysis extends AbstractAnalysis {
  val checker: TerminationChecker {
    val program: TerminationProgram
  }

  import checker._
  import context._
  import program._
  import program.trees._

  type Duration = Long
  type Record = (TerminationGuarantee, Duration)
  val results: Map[FunDef, Record]
  val sources: Set[Identifier] // set of functions that were considered for the analysis

  override val name: String = TerminationComponent.name

  override type Report = TerminationReport

  override def toReport = new TerminationReport(records, sources)

  private lazy val records = results.toSeq map { case (fd, (g, time)) =>
    TerminationReport.Record(fd.id, fd.getPos, time, status(g), verdict(g, fd), kind(g), derivedFrom = fd.source)
  }

  private def kind(g: TerminationGuarantee): String = g match {
    case checker.NotWellFormed(_) => "ill-formed"
    case checker.LoopsGivenInputs(_, _) => "non-terminating loop"
    case checker.MaybeLoopsGivenInputs(_, _) => "possibly non-terminating loop"
    case checker.CallsNonTerminating(_) => "non-terminating call"
    case checker.DecreasesFailed => "failed decreases check"
    case checker.Terminates(_) => "terminates"
    case checker.NoGuarantee => "no guarantee"
  }

  private def verdict(g: TerminationGuarantee, fd: FunDef): String = g match {
    case checker.NotWellFormed(adts) =>
      s"Refers to ill-formed adts: ${adts.map(_.id.asString).mkString(",")}"
    case checker.LoopsGivenInputs(reason, args) =>
      s"Non-terminating for call: ${fd.id.asString}(${args.map(_.asString).mkString(",")})"
    case checker.MaybeLoopsGivenInputs(reason, args) =>
      s"Possibly non-terminating for call: ${fd.id.asString}(${args.map(_.asString).mkString(",")})"
    case checker.CallsNonTerminating(fds) =>
      s"Calls non-terminating functions: ${fds.map(_.id.asString).mkString(",")}"
    case checker.DecreasesFailed =>
      s"Failed decreases check"
    case checker.Terminates(reason) =>
      s"Terminates ($reason)"
    case checker.NoGuarantee =>
      "No guarantee"
  }

  private def status(g: TerminationGuarantee): TerminationReport.Status = g match {
    case checker.NoGuarantee => TerminationReport.Unknown
    case checker.Terminates(_) => TerminationReport.Terminating
    case _ => TerminationReport.NonTerminating
  }

}

