/* Copyright 2009-2016 EPFL, Lausanne */

package stainless
package frontends.dotc

import dotty.tools.dotc.ast.Trees._
import dotty.tools.dotc.core.Phases._
import dotty.tools.dotc.core.Contexts._

import extraction.xlang.{ trees => xt }
import frontend.MasterCallBack

class StainlessExtraction(inoxCtx: inox.Context, callback: MasterCallBack, cache: SymbolsContext) extends Phase {

  def phaseName: String = "stainless extraction"

  def run(implicit ctx: Context): Unit = {
    val extraction = new CodeExtraction(inoxCtx, cache)
    import extraction.{ ctx => _, _ }

    val unit = ctx.compilationUnit
    val tree = unit.tpdTree
    val (id, stats) = tree match {
      case pd @ PackageDef(refTree, lst) =>
        val id = lst.collectFirst { case PackageDef(ref, stats) => ref } match {
          case Some(ref) => extractRef(ref)
          case None => FreshIdentifier(unit.source.file.name.replaceFirst("[.][^.]+$", ""))
        }
        (id, pd.stats)
      case _ => outOfSubsetError(tree, "Unexpected unit body")
    }

    val (imports, unitClasses, unitFunctions, subs, classes, functions) = extraction.extractStatic(stats)
    assert(unitFunctions.isEmpty, "Packages shouldn't contain functions")

    val file = unit.source.file.absolute.path
    val isLibrary = Main.libraryFiles contains file
    val xtUnit = xt.UnitDef(id, imports, unitClasses, subs, !isLibrary)

    callback(file, xtUnit, classes, functions)
  }

}

