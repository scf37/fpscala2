package me.scf37.fpscala2
import me.scf37.fpscala2.model.Todo
import _root_.tethys._
import _root_.tethys.jackson._
import me.scf37.fpscala2.controller.TodoRequest

object Test {
  def main(args: Array[String]): Unit = {
    println(""" {"text": "hello"} """.jsonAs[TodoRequest])
  }
}
