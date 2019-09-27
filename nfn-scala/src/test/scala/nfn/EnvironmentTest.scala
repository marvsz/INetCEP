package nfn

import ccn.packet.Content
import com.typesafe.config.{Config, ConfigFactory}
import org.scalatest.{BeforeAndAfterEach, SequentialNestedSuiteExecution}
import nfn.service._
import node.LocalNodeFactory
import org.scalatest._
import akka.actor._
import ccn.packet._
import config.StaticConfig
import lambdacalculus.parser.ast.Call
import lambdacalculus.parser.ast.LambdaDSL.{stringToExpr, _}
import nfn.LambdaNFNImplicits._
import org.scalatest.time.{Millis, Seconds, Span}

import scala.sys.process._
import org.junit.Test
import lambdacalculus.parser.ast.Expr
import node.LocalNode

import scala.concurrent.Future

class EnvironmentTest extends ExpressionTester with SequentialNestedSuiteExecution with BeforeAndAfterEach{

  implicit val conf: Config = ConfigFactory.load()

  (1 to 6) map { expTest }

  def expTest(n: Int)={
    s"experiment $n" should "result in corresponding result in content Object" in{
      doEnvironmentTest(n)
    }
  }
  def doEnvironmentTest(expNum: Int)={
    //System.out.println(s"Test")
    val node1 = LocalNodeFactory.forId(1)
    val node2 = LocalNodeFactory.forId(2)
    val node3 = LocalNodeFactory.forId(3)
    val node4 = LocalNodeFactory.forId(4)
    val node5 = LocalNodeFactory.forId(5)
    val node6 = LocalNodeFactory.forId(6)
    val node7 = LocalNodeFactory.forId(7)

    val nodes = List(node1, node2, node3, node4, node5, node6, node7)

    node1 <~> node2
    node1.registerPrefixToNodes(node2,List(node3,node4,node5))
    node2 <~> node3
    node3.registerPrefixToNodes(node2,List(node1,node4,node5))
    node2 <~> node4
    node2.registerPrefixToNodes(node4,List(node7))
    node4.registerPrefixToNodes(node2,List(node1,node3,node5))
    node2 <~> node5
    node2.registerPrefixToNodes(node5,List(node6))
    node5.registerPrefixToNodes(node2,List(node1,node3,node4))
    node4 <~> node7
    node7.registerPrefixToNodes(node4,List(node2))
    node5 <~> node6
    node6.registerPrefixToNodes(node5,List(node2))

    val wPrefix = new Window().ccnName
    val jPrefix = new Concatenate().ccnName
    val fPrefix = new nfn.service.Filter().ccnName
    val hPrefix = new Heatmap().ccnName
    val pPrefix = new Prediction2().ccnName

    node1.publishService(new Window)
    node1.publishService(new Concatenate)
    node1.publishService(new nfn.service.Filter)
    node1.publishService(new Heatmap)
    node1.publishService(new Prediction2)

    node2.publishService(new Window)
    node2.publishService(new Concatenate)
    node2.publishService(new nfn.service.Filter)
    node2.publishService(new Heatmap)
    node2.publishService(new Prediction2)

    node3.publishService(new Window)
    node3.publishService(new Concatenate)
    node3.publishService(new nfn.service.Filter)
    node3.publishService(new Heatmap)
    node3.publishService(new Prediction2)

    node4.publishService(new Window)
    node4.publishService(new Concatenate)
    node4.publishService(new nfn.service.Filter)
    node4.publishService(new Heatmap)
    node4.publishService(new Prediction2)

    node5.publishService(new Window)
    node5.publishService(new Concatenate)
    node5.publishService(new nfn.service.Filter)
    node5.publishService(new Heatmap)
    node5.publishService(new Prediction2)

    node6.publishService(new Window)
    node6.publishService(new Concatenate)
    node6.publishService(new nfn.service.Filter)
    node6.publishService(new Heatmap)
    node6.publishService(new Prediction2)

    node7.publishService(new Window)
    node7.publishService(new Concatenate)
    node7.publishService(new nfn.service.Filter)
    node7.publishService(new Heatmap)
    node7.publishService(new Prediction2)

    node1.registerPrefix(wPrefix, node2)
    node1.registerPrefix(jPrefix, node2)
    node1.registerPrefix(fPrefix, node2)
    node1.registerPrefix(hPrefix, node2)
    node1.registerPrefix(pPrefix, node2)

    node2.registerPrefix(wPrefix, node1)
    node2.registerPrefix(jPrefix, node1)
    node2.registerPrefix(fPrefix, node1)
    node2.registerPrefix(hPrefix, node1)
    node2.registerPrefix(pPrefix, node1)

    node2.registerPrefix(wPrefix, node3)
    node2.registerPrefix(jPrefix, node3)
    node2.registerPrefix(fPrefix, node3)
    node2.registerPrefix(hPrefix, node3)
    node2.registerPrefix(pPrefix, node3)

    node2.registerPrefix(wPrefix, node4)
    node2.registerPrefix(jPrefix, node4)
    node2.registerPrefix(fPrefix, node4)
    node2.registerPrefix(hPrefix, node4)
    node2.registerPrefix(pPrefix, node4)

    node2.registerPrefix(wPrefix, node5)
    node2.registerPrefix(jPrefix, node5)
    node2.registerPrefix(fPrefix, node5)
    node2.registerPrefix(hPrefix, node5)
    node2.registerPrefix(pPrefix, node5)

    node3.registerPrefix(wPrefix, node2)
    node3.registerPrefix(jPrefix, node2)
    node3.registerPrefix(fPrefix, node2)
    node3.registerPrefix(hPrefix, node2)
    node3.registerPrefix(pPrefix, node2)

    node4.registerPrefix(wPrefix, node2)
    node4.registerPrefix(jPrefix, node2)
    node4.registerPrefix(fPrefix, node2)
    node4.registerPrefix(hPrefix, node2)
    node4.registerPrefix(pPrefix, node2)

    node4.registerPrefix(wPrefix, node7)
    node4.registerPrefix(jPrefix, node7)
    node4.registerPrefix(fPrefix, node7)
    node4.registerPrefix(hPrefix, node7)
    node4.registerPrefix(pPrefix, node7)

    node7.registerPrefix(wPrefix, node4)
    node7.registerPrefix(jPrefix, node4)
    node7.registerPrefix(fPrefix, node4)
    node7.registerPrefix(hPrefix, node4)
    node7.registerPrefix(pPrefix, node4)

    node5.registerPrefix(wPrefix, node2)
    node5.registerPrefix(jPrefix, node2)
    node5.registerPrefix(fPrefix, node2)
    node5.registerPrefix(hPrefix, node2)
    node5.registerPrefix(pPrefix, node2)

    node5.registerPrefix(wPrefix, node6)
    node5.registerPrefix(jPrefix, node6)
    node5.registerPrefix(fPrefix, node6)
    node5.registerPrefix(hPrefix, node6)
    node5.registerPrefix(pPrefix, node6)

    node6.registerPrefix(wPrefix, node5)
    node6.registerPrefix(jPrefix, node5)
    node6.registerPrefix(fPrefix, node5)
    node6.registerPrefix(hPrefix, node5)
    node6.registerPrefix(pPrefix, node5)

    Thread.sleep(1000)

    val wc = new Window()

    val exp1 = wc call List(stringToExpr("timestamp"), stringToExpr("data"), stringToExpr("plug0"), stringToExpr("5"), stringToExpr("M"))
    val res1 = "720838289,1379082091,17.209,1,0,0,0\n1720847151,1379082096,21.088,1,0,0,0\n1720858898,1379082102,17.693,1,0,0,0\n1720868378,1379082107,19.665,1,0,0,0\n1720877407,1379082112,17.716,1,0,0,0\n1720888342,1379082118,17.27,1,0,0,0\n1720897833,1379082123,19.375,1,0,0,0\n1720908768,1379082129,18.503,1,0,0,0\n1720917546,1379082134,20.18,1,0,0,0\n1720928480,1379082140,18.9,1,0,0,0\n1720937244,1379082145,20.233,1,0,0,0\n1720946360,1379082150,20.093,1,0,0,0\n1720957294,1379082156,19.375,1,0,0,0\n1720967319,1379082161,18.073,1,0,0,0\n1720976140,1379082166,19.877,1,0,0,0\n1720986777,1379082172,19.375,1,0,0,0\n1720994929,1379082177,17.903,1,0,0,0\n1721006473,1379082183,20.357,1,0,0,0\n1721016026,1379082188,20.142,1,0,0,0\n1721026815,1379082194,19.375,1,0,0,0\n1721036515,1379082199,20.741,1,0,0,0\n1721045481,1379082204,19.183,1,0,0,0\n1721055976,1379082210,20.093,1,0,0,0\n1721065971,1379082215,19.375,1,0,0,0\n1721075759,1379082221,21.392,1,0,0,0\n1721084794,1379082226,18.207,1,0,0,0\n1721094505,1379082231,21.524,1,0,0,0\n1721106099,1379082237,21.496,1,0,0,0\n1721114522,1379082242,20.093,1,0,0,0\n1721125636,1379082248,21.946,1,0,0,0\n1721134503,1379082253,19.737,1,0,0,0\n1721144688,1379082259,19.725,1,0,0,0\n1721153794,1379082264,21.33,1,0,0,0\n1721162639,1379082269,19.862,1,0,0,0\n1721173921,1379082275,19.375,1,0,0,0\n1721182170,1379082280,20.961,1,0,0,0\n1721191258,1379082285,21.844,1,0,0,0\n1721201818,1379082291,20.093,1,0,0,0\n1721211844,1379082296,20.093,1,0,0,0\n1721222464,1379082302,20.093,1,0,0,0\n1721231274,1379082307,18.327,1,0,0,0\n1721241054,1379082312,20.743,1,0,0,0\n1721250964,1379082318,18.192,1,0,0,0\n1721261104,1379082323,19.864,1,0,0,0\n1721271670,1379082329,19.491,1,0,0,0\n1721281046,1379082334,20.093,1,0,0,0\n1721291980,1379082340,18.529,1,0,0,0\n1721301120,1379082345,20.482,1,0,0,0\n1721310166,1379082350,21.009,1,0,0,0\n1721321504,1379082356,19.236,1,0,0,0\n1721329626,1379082361,19.336,1,0,0,0\n1721341258,1379082367,21.87,1,0,0,0\n1721350294,1379082372,17.674,1,0,0,0\n1721360910,1379082378,20.86,1,0,0,0\n1721370036,1379082383,20.093,1,0,0,0\n1721379102,1379082388,19.394,1,0,0,01720838289,1379082091,17.209,1,0,0,0\n1720847151,1379082096,21.088,1,0,0,0\n1720858898,1379082102,17.693,1,0,0,0\n1720868378,1379082107,19.665,1,0,0,0\n1720877407,1379082112,17.716,1,0,0,0\n1720888342,1379082118,17.27,1,0,0,0\n1720897833,1379082123,19.375,1,0,0,0\n1720908768,1379082129,18.503,1,0,0,0\n1720917546,1379082134,20.18,1,0,0,0\n1720928480,1379082140,18.9,1,0,0,0\n1720937244,1379082145,20.233,1,0,0,0\n1720946360,1379082150,20.093,1,0,0,0\n1720957294,1379082156,19.375,1,0,0,0\n1720967319,1379082161,18.073,1,0,0,0\n1720976140,1379082166,19.877,1,0,0,0\n1720986777,1379082172,19.375,1,0,0,0\n1720994929,1379082177,17.903,1,0,0,0\n1721006473,1379082183,20.357,1,0,0,0\n1721016026,1379082188,20.142,1,0,0,0\n1721026815,1379082194,19.375,1,0,0,0\n1721036515,1379082199,20.741,1,0,0,0\n1721045481,1379082204,19.183,1,0,0,0\n1721055976,1379082210,20.093,1,0,0,0\n1721065971,1379082215,19.375,1,0,0,0\n1721075759,1379082221,21.392,1,0,0,0\n1721084794,1379082226,18.207,1,0,0,0\n1721094505,1379082231,21.524,1,0,0,0\n1721106099,1379082237,21.496,1,0,0,0\n1721114522,1379082242,20.093,1,0,0,0\n1721125636,1379082248,21.946,1,0,0,0\n1721134503,1379082253,19.737,1,0,0,0\n1721144688,1379082259,19.725,1,0,0,0\n1721153794,1379082264,21.33,1,0,0,0\n1721162639,1379082269,19.862,1,0,0,0\n1721173921,1379082275,19.375,1,0,0,0\n1721182170,1379082280,20.961,1,0,0,0\n1721191258,1379082285,21.844,1,0,0,0\n1721201818,1379082291,20.093,1,0,0,0\n1721211844,1379082296,20.093,1,0,0,0\n1721222464,1379082302,20.093,1,0,0,0\n1721231274,1379082307,18.327,1,0,0,0\n1721241054,1379082312,20.743,1,0,0,0\n1721250964,1379082318,18.192,1,0,0,0\n1721261104,1379082323,19.864,1,0,0,0\n1721271670,1379082329,19.491,1,0,0,0\n1721281046,1379082334,20.093,1,0,0,0\n1721291980,1379082340,18.529,1,0,0,0\n1721301120,1379082345,20.482,1,0,0,0\n1721310166,1379082350,21.009,1,0,0,0\n1721321504,1379082356,19.236,1,0,0,0\n1721329626,1379082361,19.336,1,0,0,0\n1721341258,1379082367,21.87,1,0,0,0\n1721350294,1379082372,17.674,1,0,0,0\n1721360910,1379082378,20.86,1,0,0,0\n1721370036,1379082383,20.093,1,0,0,0\n1721379102,1379082388,19.394,1,0,0,0"

   // val res1 = Seq("720838289,1379082091,17.209,1,0,0,0","720838289,1379082091,17.209,1,0,0,0").toString()

    System.out.println("Output is")


   implicit val nodeToSendInterestsTo = node1
    System.out.println(EvaluateExpr(exp1))

    expNum match{
      case 1 => doExp(exp1, testExpected(res1))
      case _=> throw new Exception(s"expNum can only be 1 and not $expNum")
    }

    nodes foreach{_.shutdown()}

  }

  def EvaluateExpr(exprToDo: Expr, useThunks: Boolean = false)(implicit node:LocalNode)={
    implicit val us = useThunks
    import LambdaNFNImplicits._
    val f:Future[Content] = node ? exprToDo
    implicit val patienceConfig = PatienceConfig(Span(StaticConfig.defaultTimeoutDuration.toMillis,Millis),Span(2000,Millis))
    whenReady(f){content => new String(content.data) + "DSFSDF"
    }
  }
}
