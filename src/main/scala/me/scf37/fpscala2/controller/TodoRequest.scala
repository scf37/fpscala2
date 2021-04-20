package me.scf37.fpscala2.controller

import java.util.regex.Pattern
import me.scf37.fpscala2.tethys.JsonReader3

case class TodoRequest(
  text: String
) derives JsonReader3


