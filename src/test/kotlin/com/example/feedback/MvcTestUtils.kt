package com.example.feedback

import com.jayway.jsonpath.JsonPath
import com.example.feedback.models.BaseEntityId
import org.springframework.test.web.servlet.MvcResult

fun MvcResult.getEntityId() =
    JsonPath.read<Int>(response.contentAsString, "$.id").toLong() as BaseEntityId
