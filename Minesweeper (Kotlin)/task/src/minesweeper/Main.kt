package minesweeper

import kotlin.collections.MutableList
import kotlin.collections.count
import kotlin.random.Random

const val FIELD_SIZE = 9
const val LEGEND_BUFFER = 3
const val MINE = "X"
const val UNEXPLORED = "."
const val FREE = "/"
const val MARK = "*"

enum class Action {
    MINE, FREE;

    companion object {
        fun from(action: String): Action = when (action) {
            "mine" -> MINE
            "free" -> FREE
            else -> throw IllegalArgumentException("Must be mine or free, gave: $action")
        }
    }
}

class Input(val x: Int, val y: Int, val action: Action)
class Tile(val x: Int, val y: Int) {
    var isMine = false
    var isMarked = false
    var adjacentMines = 0
    var isExplored = false
    var isLegend = false
    var isFree = false
    var isHint = false
    var text: String = UNEXPLORED

}

class Minefield(
    val size: Int,
    val mines: Int,
) {
    lateinit var field: MutableList<MutableList<Tile>>
    var hasExploded = false
    var isDemined = false
    var firstAttempt = true
    val directions = listOf(
        Pair(-1, -1),
        Pair(-1, 0),
        Pair(-1, 1),
        Pair(0, -1),
        Pair(0, 1),
        Pair(1, -1),
        Pair(1, 0),
        Pair(1, 1)
    )

    fun initializeField() {
        field = MutableList(size + LEGEND_BUFFER) { y ->
            MutableList(size + LEGEND_BUFFER) { x ->
                Tile(
                    x, y
                )
            }
        }
        field[0][0].isLegend = true
        field[0][0].text = " "


        for (x in 0 until size + LEGEND_BUFFER) {
            for (y in 0 until size + LEGEND_BUFFER) {
                if (x == 0 && y == 0) continue
                if (y == 1 || y == size + LEGEND_BUFFER - 1) {
                    field[x][y].let {
                        it.isLegend = true
                        it.text = "|"
                    }

                } else if (x == 1 || x == size + LEGEND_BUFFER - 1) {
                    field[x][y].let {
                        it.isLegend = true
                        it.text = "-"
                    }
                } else if (x == 0) field[x][y].let {
                    it.isLegend = true; it.text = (y - 1).toString()
                }
                else if (y == 0) field[x][y].let {
                    it.isLegend = true; it.text = (x - 1).toString()
                }


            }

        }
    }

    fun placeMines(
    ) {
        var minesPlaced = 0
        while (minesPlaced < mines) {
            val x = Random.nextInt(2, size + LEGEND_BUFFER - 1)
            val y = Random.nextInt(2, size + LEGEND_BUFFER - 1)
            if (!field[x][y].isMine && !field[x][y].isFree) {
                field[x][y].isMine = true
                minesPlaced++
            }
        }
    }

    fun calculateHints() {
        for (x in 2 until size + LEGEND_BUFFER - 1) {
            for (y in 2 until size + LEGEND_BUFFER - 1) {
                if (field[x][y].isMine) continue

                field[x][y].adjacentMines = directions.count { (dx, dy) ->
                    val newX = x + dx
                    val newY = y + dy
                    newX in 2 until size + LEGEND_BUFFER - 1 && newY in 2 until size + LEGEND_BUFFER - 1 && field[newX][newY].isMine
                }
                field[x][y].isHint = field[x][y].adjacentMines > 0
            }
        }
    }

    fun drawField() {
        field.forEach { row ->
            println(row.joinToString("") {
                it.text
            })
        }
    }

    fun refreshText() {
        field.forEach { row ->
            row.forEach { tile ->
                tile.text = when {
                    tile.isLegend -> tile.text
                    tile.isMarked -> MARK
                    tile.isMine && hasExploded -> MINE
                    tile.isExplored && tile.adjacentMines > 0 -> tile.adjacentMines.toString()
                    tile.isExplored && tile.adjacentMines == 0 -> FREE
                    else -> UNEXPLORED
                }
            }
        }
    }

    fun handleAction(
        input: Input,
    ) {
        if (input.action == Action.FREE) {
            if (firstAttempt) {
                field[input.x][input.y].isFree = true
                firstAttempt = false
                placeMines()
                calculateHints()
                explore(input.x, input.y)
            } else if (field[input.x][input.y].isMine) hasExploded = true
            else {
                explore(
                    input.x, input.y
                )
            }
        } else {
            field[input.x][input.y].let { it.isMarked = !it.isMarked }

        }


        if (field.sumOf { row -> row.count() { it.isMarked } } == mines && field.sumOf { row -> row.count() { it.isMarked && it.isMine } } == mines) isDemined =
            true
        else if (field.sumOf { row -> row.count() { !it.isExplored } } == mines && field.sumOf { row -> row.count() { !it.isExplored && it.isMine } } == mines) isDemined =
            true
    }

    private fun explore(x: Int, y: Int) {
        if (x in 2 until size + LEGEND_BUFFER - 1 && y in 2 until size + LEGEND_BUFFER - 1) {
            val tile = field[x][y]
            if (tile.isMine || tile.isExplored) return
            tile.isFree = true
            tile.isExplored = true
            tile.isMarked = false

            if (tile.adjacentMines == 0) directions.forEach { (dx, dy) ->
                explore(
                    x + dx, y + dy
                )
            }
        }
    }
}


fun main() {
    println("How many mines do you want on the field?")
    val minesCount = readln().toIntOrNull() ?: 0
    val minefield = Minefield(FIELD_SIZE, minesCount)
    minefield.initializeField()
    minefield.drawField()
    while (!minefield.hasExploded && !minefield.isDemined) {
        println("Set/unset mines marks or claim a cell as free: ")
        val input: Input = readln().split(" ").let {
            Input(
                it[1].toInt() + 1, it[0].toInt() + 1, Action.from(it[2])
            )
        }
        minefield.handleAction(input)
        minefield.refreshText()
        minefield.drawField()

        minefield.field[input.x][input.y].let {

            if (minefield.hasExploded) println("You stepped on a mine and failed!")
            else if (minefield.isDemined) println("Congratulations! You found all the mines!")

        }

    }
    }


