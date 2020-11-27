package coverosR3z.domainobjects

import coverosR3z.misc.checkParseToInt

const val maximumProjectsCount = 100_000_000
private const val maxProjectErrorMsg = "No project id allowed over $maximumProjectsCount"
private const val emptyProjectNameMsg = "Makes no sense to have an empty project name"
private const val minIdMsg = "Valid identifier values are 1 or above"
const val projectIdNotNullMsg = "The project id must not be null"
const val projectNameNotNullMsg = "The project name must not be null"
const val projectIdNotBlankMsg = "The project id must not be blank"

/**
 * This is used to represent no project - just to avoid using null for a project
 * It's a typed null, essentially
 */
val NO_PROJECT = Project(ProjectId(maximumProjectsCount-1), ProjectName("THIS REPRESENTS NO PROJECT"))

/**
 * When we just have a name (like when adding a new project, or searching)
 */
data class ProjectName(val value: String) {
    init {
        require(value.isNotEmpty()) {emptyProjectNameMsg}
    }

    companion object {
        fun make(value: String?) : ProjectName {
            val valueNotNull = checkNotNull(value) {projectNameNotNullMsg}
            return ProjectName(valueNotNull)
        }
    }
}

data class ProjectId(val value: Int) {
    init {
        require(value > 0) {minIdMsg}
        require(value < maximumProjectsCount) { maxProjectErrorMsg }
    }

    companion object {
        fun make(value: String?) : ProjectId {
            val id = checkNotNull(value) {projectIdNotNullMsg}
            require(id.isNotBlank()) {projectIdNotBlankMsg}
            val idInt = checkParseToInt(id)
            return ProjectId(idInt)
        }
    }
}

/**
 * A full Project object
 */
data class Project(val id: ProjectId, val name: ProjectName)

