package com.workoutlog.app

import com.workoutlog.app.model.AppData
import com.workoutlog.app.model.CardioEntry
import com.workoutlog.app.model.DayLog
import com.workoutlog.app.model.ExerciseEntry
import com.workoutlog.app.model.FoodItem
import com.workoutlog.app.model.Goal
import com.workoutlog.app.model.Meal
import com.workoutlog.app.model.Measurement
import com.workoutlog.app.model.SetEntry
import com.workoutlog.app.model.Settings
import com.workoutlog.app.model.Tracker
import java.time.LocalDate
import kotlin.random.Random

/** Realistic fake data used only by tests/screenshots. */
object SampleData {
    val today: LocalDate = LocalDate.now()

    fun build(): AppData {
        val r = Random(42)
        val logs = mutableMapOf<String, DayLog>()
        for (i in 0 until 100) {
            val d = today.minusDays((99 - i).toLong())
            val isLift = d.dayOfWeek.value in listOf(1, 3, 5)
            val isRun = d.dayOfWeek.value == 6
            val bench = 125 + i * 0.22
            val exercises = if (isLift) listOf(
                ExerciseEntry("Bench press", List(6) { s -> if (s < 4) SetEntry(((bench - (3 - s) * 5).toInt() / 5 * 5).toString(), listOf("8", "6", "5", "4")[s]) else SetEntry() }),
                ExerciseEntry("Pull-ups", List(6) { s -> if (s < 3) SetEntry("BW", (1 + i / 30 + (if (s == 0) 1 else 0)).toString()) else SetEntry() }),
                ExerciseEntry("Squat", List(6) { s -> if (s < 4) SetEntry((155 + i / 2 / 5 * 5).toString(), "5") else SetEntry() }),
            ) else listOf(ExerciseEntry())
            val cardio = if (isRun || isLift) listOf(
                CardioEntry("Run", if (isRun) "${15 - i / 40}:${(30 + r.nextInt(25)).toString().padStart(2, '0')}" else "10:00", if (isRun) "1.5" else "1", if (isRun) "190" else "120"),
                CardioEntry(),
            ) else listOf(CardioEntry(), CardioEntry())
            val meals = listOf(
                Meal("Breakfast", "7:00", listOf(
                    FoodItem("Oatmeal", "1 cup", "300", "10", "6", "54", "1"),
                    FoodItem("Eggs", "3", "210", "18", "15", "1", "0"),
                )),
                Meal("Lunch", "12:30", listOf(
                    FoodItem("Chicken breast", "6 oz", "280", "52", "6", "0", "0"),
                    FoodItem("Rice", "1 cup", "205", "4", "0", "45", "0"),
                )),
                Meal("Dinner", "18:30", listOf(
                    FoodItem("Salmon", "5 oz", "290", "32", "17", "0", "0"),
                    FoodItem("Sweet potato", "1", "${100 + r.nextInt(80)}", "2", "0", "24", "7"),
                )),
                Meal("Snacks", "", listOf(FoodItem("Protein shake", "1 scoop", "120", "24", "1", "3", "1"))),
            )
            logs[d.toString()] = DayLog(
                date = d.toString(),
                workoutName = when { isLift -> "Upper / Lower"; isRun -> "Saturday run"; else -> "" },
                warmUp = isLift, coolDown = isLift,
                water = (60 + r.nextInt(40)).toString(),
                sleep = listOf("6", "6.5", "7", "7.5", "8")[r.nextInt(5)],
                weight = String.format("%.1f", 192 - i * 0.06 + r.nextDouble() * 1.2),
                duration = if (isLift) "60" else if (isRun) "25" else "",
                exercises = exercises, cardio = cardio,
                notes = if (isLift && i % 7 == 0) "Felt strong today. Bench moved fast." else "",
                rating = if (isLift || isRun) listOf(60, 80, 80, 100)[r.nextInt(4)] else 0,
                mood = 2 + r.nextInt(4), energy = 2 + r.nextInt(4), activity = 1 + r.nextInt(5),
                bodyFat = "", caffeine = r.nextInt(3).toString(), fruit = (1 + r.nextInt(3)).toString(), veggies = (2 + r.nextInt(3)).toString(),
                sugarFree = r.nextBoolean(), glutenFree = false, lactoseFree = false, dairyFree = r.nextInt(5) == 0,
                meals = meals,
            )
        }
        val measurements = (0 until 7).map { k ->
            val d = today.minusDays((90 - k * 15).toLong())
            Measurement(
                id = k.toLong(), date = d.toString(), neck = "15.5", chest = (41 + k * 0.2).toString(), biceps = (14 + k * 0.1).toString(),
                waist = (35 - k * 0.3).toString(), hips = "39", thigh = "23", calf = "15", weight = (192 - k * 0.9).toString(),
                bodyFat = (21 - k * 0.5).toString(),
            )
        }
        val trackers = listOf(
            Tracker(1, "Bench max", "lbs", List(33) { w -> if (w < 14) (125 + w * 2).toString() else "" }),
            Tracker(2, "1.5 mile run", "min", List(33) { w -> if (w < 14) "${15 - w / 6}:${(50 - w * 3 % 50).toString().padStart(2, '0')}" else "" }),
            Tracker(3, "Pull-ups", "reps", List(33) { w -> if (w < 14) (1 + w / 4).toString() else "" }),
        )
        return AppData(
            settings = Settings(calorieGoal = "2200", proteinGoal = "160", fatGoal = "70", carbsGoal = "220", extraGoal = "30",
                extraNutrientLabel = "Fiber", trackerStartDate = today.minusWeeks(13).toString()),
            logs = logs,
            goals = listOf(
                Goal(1, "Bench 150 lbs", "2025-08-13", "2025-12-17", "Progressive overload bench press @ YMCA and @ Academy PT"),
                Goal(2, "1.5 mile in 13:30", "2025-08-13", "2025-12-17", "Run often, during academy and on Saturdays outside"),
                Goal(3, "5 Pull-ups", "2025-08-13", "2025-12-17", "", "Got 3 clean reps in November"),
            ),
            measurements = measurements,
            trackers = trackers,
        )
    }
}
