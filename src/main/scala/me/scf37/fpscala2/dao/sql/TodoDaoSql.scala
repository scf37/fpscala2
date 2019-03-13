package me.scf37.fpscala2.dao.sql

import java.sql.ResultSet

import cats.Monad
import cats.effect.IO
import cats.effect.Resource
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.db.Db
import me.scf37.fpscala2.model.Todo
import cats.implicits._

class TodoDaoSql[F[_]: Db: Monad] extends TodoDao[F] {

  override def list(): F[Seq[Todo]] = Db[F].eval { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(IO(conn.createStatement()))
      rs <- Resource.fromAutoCloseable(IO(st.executeQuery("select uid, text from item")))
    } yield {
      rsIterator(rs)(parse).toList
    }

    r.use(IO.apply(_)).unsafeRunSync
  }

  override def save(todo: Todo): F[Todo] = for {
    updated <- update(todo)
    r <- updated.fold(create(todo))(Monad[F].pure)
  } yield r

  override def delete(id: String): F[Boolean] = Db[F].eval { conn =>
    ???
  }

  override def get(id: String): F[Option[Todo]] = Db[F].eval { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(IO(conn.prepareStatement("select uid, text from item where uid=?")))
      _ = st.setString(1, id)
      rs <- Resource.fromAutoCloseable(IO(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).toList.headOption
    }

    r.use(IO.apply(_)).unsafeRunSync
  }

  private def create(todo: Todo): F[Todo] = Db[F].eval { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(IO(conn.prepareStatement(
        "insert into item(id, uid, text, created, updated)" +
          " values(nextval('item_id_s'), ?, ?, now(), now()) returning *"
      )))
      _ = st.setString(1, todo.id)
      _ = st.setString(2, todo.text)
      rs <- Resource.fromAutoCloseable(IO(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).next()
    }

    r.use(IO.apply(_)).unsafeRunSync
  }

  private def update(todo: Todo): F[Option[Todo]] = Db[F].eval { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(IO(conn.prepareStatement(
        "update item set uid=?, text=? where uid=? returning *"
      )))
      _ = st.setString(1, todo.id)
      _ = st.setString(2, todo.text)
      _ = st.setString(3, todo.id)
      rs <- Resource.fromAutoCloseable(IO(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).toSeq.headOption
    }

    r.use(IO.apply(_)).unsafeRunSync
  }

  private def parse(rs: ResultSet): Todo = Todo(
    id = rs.getString("uid"),
    text = rs.getString("text"))

  private def rsIterator[T](rs: ResultSet)(f: ResultSet => T): Iterator[T] =
    Iterator.continually(rs.next()).takeWhile(identity).map(_ => f(rs))
}
