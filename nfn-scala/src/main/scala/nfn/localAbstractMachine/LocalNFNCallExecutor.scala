package nfn.localAbstractMachine

import SACEPICN.StatesSingleton
import akka.actor.ActorRef
import lambdacalculus.machine.CallByValue.VariableMachineValue
import lambdacalculus.machine._
import nfn.service._

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}

case class LocalNFNCallExecutor(ccnWorker: ActorRef, stateHolder:StatesSingleton)(implicit execContext: ExecutionContext) extends CallExecutor {

  override def executeCall(call: String): MachineValue = {

    val futValue: Future[MachineValue] = {
      for {
        callableServ <- NFNService.parseAndFindFromName(call, stateHolder, ccnWorker)
      } yield {
        val result = Await.result(callableServ.exec,20.seconds)
        NFNValueToMachineValue.toMachineValue(result)
      }
    }
    Await.result(futValue, 20.seconds)
  }
}

object NFNValueToMachineValue {
  def toMachineValue(nfnValue: NFNValue):MachineValue =  {

    nfnValue match {
      case NFNIntValue(n) => ConstMachineValue(n)
      case NFNNameValue(name) => VariableMachineValue(name.toString)
      case NFNEmptyValue() => NopMachineValue()
      case NFNListValue(values: List[NFNValue]) => ListMachineValue(values map { toMachineValue})
      case _ =>  throw new Exception(s"NFNValueToMachineValue: conversion of $nfnValue to machine value type not implemented")
    }
  }

}

