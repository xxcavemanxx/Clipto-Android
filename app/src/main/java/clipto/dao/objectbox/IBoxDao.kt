package clipto.dao.objectbox

interface IBoxDao<E> {

    fun getType(): Class<E>

    fun init()

    fun clear()

}