package ru.nsu.vmarkidonov

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

fun main() {
    System.setOut(java.io.PrintStream(System.out, true, "UTF-8"))
    val employeesInput = arrayOf<Array<Any>>(
        arrayOf("Иван Иванов", 993),
        arrayOf("Пётр Петров", 994),
        arrayOf("Алексей Сидоров", 995),
        arrayOf("Дмитрий Смирнов", 999),
        arrayOf("Сергей Кузнецов", 1000),
        arrayOf("Никита Попов", 1001),
        arrayOf("Андрей Васильев", 1993),
        arrayOf("Михаил Новиков", 1994),
        arrayOf("Владимир Фёдоров", 1995),
        arrayOf("Eгор Морозов", 1996),
        arrayOf("Максим Волков", 2000),
        arrayOf("Роман Алексеев", 2001),
        arrayOf("Артём Лебедев", 2002),
        arrayOf("Кирилл Семёнов", 2005),
        arrayOf("Олег Егоров", 2006),
        arrayOf("Иван Петров", 2007),
        arrayOf("Пётр Иванов", 2008)
    )

    val currentDate = LocalDate.now()

    val resultMatrix: Array<Array<Any>> = getAnniversariesForWeek(currentDate, employeesInput)

    val dateFormatter = DateTimeFormatter.ofPattern("dd.MM")
    println("Date\tText")
    var lastDateStr = ""
    resultMatrix.forEach { row ->
        val dateStr = (row[0] as LocalDate).format(dateFormatter)
        val displayDate = if (dateStr == lastDateStr) "     " else dateStr.also { lastDateStr = it }
        println("$displayDate\t${row[1]}")
    }
}

fun getAnniversariesForWeek(currentDate: LocalDate, employees: Array<Array<Any>>): Array<Array<Any>> {
    val monday = currentDate.with(DayOfWeek.MONDAY)

    return (0..6).flatMap { i ->
        val targetDate = monday.plusDays(i.toLong())
        val delta = ChronoUnit.DAYS.between(currentDate, targetDate)

        employees.filter { row ->
            val days = row[1] as Int
            val targetDays = days + delta
            targetDays >= 1000 && targetDays % 1000 == 0.toLong()
        }.map { row ->
            val name = row[0] as String
            val days = row[1] as Int
            arrayOf<Any>(targetDate, "$name – ${days + delta} дней")
        }
    }.toTypedArray()
}