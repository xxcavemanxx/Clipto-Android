package clipto.dao.objectbox

import clipto.dao.objectbox.model.UserBox
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserBoxDao @Inject constructor() : AbstractBoxDao<UserBox>() {

    override fun getType(): Class<UserBox> = UserBox::class.java

    fun getUser(): UserBox? = box.all.lastOrNull()

    fun login(user: UserBox): UserBox {
        box.removeAll()
        box.put(user)
        return user
    }

    fun save(user: UserBox): UserBox {
        box.put(user)
        return user
    }

}