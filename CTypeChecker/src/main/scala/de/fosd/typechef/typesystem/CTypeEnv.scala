package de.fosd.typechef.typesystem

import de.fosd.typechef.parser.c._
import de.fosd.typechef.parser._
import de.fosd.typechef.featureexpr.FeatureExpr
import org.kiama.attribution.DynamicAttribution._
import org.kiama._

trait CTypeEnv extends CTypes with ASTNavigation with CDeclTyping {

    //Variable-Typing Context: identifier to its non-void wellformed type
    type VarTypingContext = Map[String, CType]

    //Function-Typing Context: identifer to function types
    type FunTypingContext = Map[String, CFunction]

    //Type synonyms with typedef
    type TypeDefEnv = Map[String, CType]

    class StructEnv(val env: Map[String, Seq[(String, FeatureExpr, CType)]]) {
        def this() = this (Map())
        def contains(name: String) = env contains name
        def add(name: String, attributes: Seq[(String, FeatureExpr, CType)]) =
        //TODO check distinct attribute names in each variant
            new StructEnv(env + (name -> (env.getOrElse(name, Seq()) ++ attributes)))
        def get(name: String): Seq[(String, FeatureExpr, CType)] = env(name)
    }


    val structEnv: AST ==> StructEnv = attr {
        case e@ADeclaration(decls, _) =>
            decls.foldRight(outerStructEnv(e))({
                case (Opt(_, a), b) => val s = a -> struct; if (s.isDefined) b.add(s.get._1, s.get._2) else b;
            })
        case e: AST => outerStructEnv(e)
    }

    val struct: AST ==> Option[(String, Seq[(String, FeatureExpr, CType)])] = attr {
        case e@StructOrUnionSpecifier(_, Some(Id(name)), attributes) =>
        //TODO variability
            Some(name, readAttributes(attributes.map(_.entry)))
        case _ => None
    }

    def readAttributes(attrs: List[StructDeclaration]): Seq[(String, FeatureExpr, CType)] = {
        //TODO variability
        var result = Seq[(String, FeatureExpr, CType)]()
        for (attr <- attrs; Opt(f, strDecl) <- attr.declaratorList) strDecl match {
            case StructDeclarator(decl, _, _) => result = result :+ ((decl.getName, f, declType(attr.qualifierList, decl)))
            case StructInitializer(expr, _) => //TODO check: should only occur in initializers, not in struct declarations
        }
        result
    }


    private def outerStructEnv(e: AST): StructEnv =
        outer[StructEnv](structEnv, () => new StructEnv(Map()), e)

    private def outer[T](f: AST ==> T, init: () => T, e: AST): T =
        if (e -> prevAST != null) f(e -> prevAST)
        else
        if (e -> parentAST != null) f(e -> parentAST)
        else
            init()


    def wellformed(structEnv: StructEnv, ptrEnv: PtrEnv, ctype: CType): Boolean = {
        val wf = wellformed(structEnv, ptrEnv, _: CType)
        ctype match {
            case CSigned(_) => true
            case CUnsigned(_) => true
            case CSignUnspecified(_) => true
            case CVoid() => true
            case CFloat() => true
            case CDouble() => true
            case CLongDouble() => true
            case CPointer(CStruct(s)) => ptrEnv contains s
            case CPointer(t) => wf(t)
            case CArray(t, n) => wf(t) && (t != CVoid()) && n > 0
            case CFunction(param, ret) => wf(ret) && !arrayType(ret) && (
                    param.forall(p => wf(p) && !arrayType(p) && p != CVoid()))
            case CStruct(name) => {
                val members = structEnv.env.getOrElse(name, Seq())
                //TODO variability
                val memberNames = members.map(_._1)
                val memberTypes = members.map(_._3)
                (!members.isEmpty && memberNames.distinct.size == memberNames.size &&
                        memberTypes.forall(t => {
                            t != CVoid() && wellformed(structEnv, ptrEnv + name, t)
                        }))
            }
            case CUnknown(_) => false
            case CObj(_) => false
        }
    }

}