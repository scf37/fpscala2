package me.scf37.fpscala2.dao.sql

import java.sql.ResultSet
import cats.Monad
import cats.MonadError
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.db.sql.SqlEffectLift
import me.scf37.fpscala2.model.Todo
import me.scf37.fpscala2.typeclass.SyncResource

/**
  * TodoDao implementation using jdbc Connection.
  *
  * JDBC Api is not synchronized and therefore F must be single-threaded
  *
  * @param DB typeclass to lift Connection => T function to DbEffect
  * @tparam F database effect
  */
class TodoDaoSql[F[_]](
  using DB: SqlEffectLift[F],
  ME: MonadError[F, Throwable]
) extends TodoDao[F]:

  override def list(): F[Seq[Todo]] = DB.lift { conn =>
    val r =
      for
        st <- SyncResource.fromAutoCloseable(conn.createStatement())
        rs <- SyncResource.fromAutoCloseable(st.executeQuery("select uid, text from item order by uid"))
      yield
        rsIterator(rs)(parse).toList

    r.use(_.pure)
  }

  override def save(todo: Todo): F[Todo] =
    for
      updated <- update(todo)
      r <- updated.fold(create(todo))(Monad[F].pure)
    yield r

  override def delete(id: String): F[Boolean] = DB.lift { conn =>
    val r =
      for
        st <- SyncResource.fromAutoCloseable(conn.prepareStatement("delete from item where uid=?"))
        _ = st.setString(1, id)
        rows = st.executeUpdate()
      yield
        rows > 0

    r.use(_.pure)
  }

  override def get(id: String): F[Option[Todo]] = DB.lift { conn =>
    val r =
      for
        st <- SyncResource.fromAutoCloseable(conn.prepareStatement("select uid, text from item where uid=?"))
        _ = st.setString(1, id)
        rs <- SyncResource.fromAutoCloseable(st.executeQuery())
      yield
        rsIterator(rs)(parse).toList.headOption

    r.use(_.pure)
  }

  private def create(todo: Todo): F[Todo] = DB.lift { conn =>
    val r =
      for
        st <- SyncResource.fromAutoCloseable(conn.prepareStatement(
          "insert into item(id, uid, text, created, updated)" +
            " values(nextval('item_id_s'), ?, ?, now(), now()) returning *"
        ))
        _ = st.setString(1, todo.id)
        _ = st.setString(2, todo.text)
        rs <- SyncResource.fromAutoCloseable(st.executeQuery())
      yield
        rsIterator(rs)(parse).next()

    r.use(_.pure)
  }

  private def update(todo: Todo): F[Option[Todo]] = DB.lift { conn =>
    val r =
      for
        st <- SyncResource.fromAutoCloseable(conn.prepareStatement(
          "update item set uid=?, text=? where uid=? returning *"
        ))
        _ = st.setString(1, todo.id)
        _ = st.setString(2, todo.text)
        _ = st.setString(3, todo.id)
        rs <- SyncResource.fromAutoCloseable(st.executeQuery())
      yield
        rsIterator(rs)(parse).toSeq.headOption

    r.use(_.pure)
  }

  private def parse(rs: ResultSet): Todo = Todo(
    id = rs.getString("uid"),
    text = rs.getString("text"))

  private def rsIterator[T](rs: ResultSet)(f: ResultSet => T): Iterator[T] =
    Iterator.continually(rs.next()).takeWhile(identity).map(_ => f(rs))

