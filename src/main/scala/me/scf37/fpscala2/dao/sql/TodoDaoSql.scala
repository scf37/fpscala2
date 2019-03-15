package me.scf37.fpscala2.dao.sql

import java.sql.ResultSet

import cats.Monad
import cats.effect.Resource
import cats.effect.Sync
import cats.implicits._
import me.scf37.fpscala2.dao.TodoDao
import me.scf37.fpscala2.db.Db
import me.scf37.fpscala2.model.Todo

class TodoDaoSql[DbEffect[_]: Monad, F[_]: Sync](
  implicit DB: Db[DbEffect, F]
) extends TodoDao[DbEffect] {

  override def list(): DbEffect[Seq[Todo]] = DB.lift { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(Sync[F].delay(conn.createStatement()))
      rs <- Resource.fromAutoCloseable(Sync[F].delay(st.executeQuery("select uid, text from item order by uid")))
    } yield {
      rsIterator(rs)(parse).toList
    }

    r.use(r => Sync[F].delay(r: Seq[Todo]))
  }

  override def save(todo: Todo): DbEffect[Todo] = for {
    updated <- update(todo)
    r <- updated.fold(create(todo))(Monad[DbEffect].pure)
  } yield r

  override def delete(id: String): DbEffect[Boolean] = DB.lift { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(Sync[F].delay(conn.prepareStatement("delete from item where uid=?")))
      _ = st.setString(1, id)
      rows = st.executeUpdate()
    } yield {
      rows > 0
    }

    r.use(r => Sync[F].delay(r))
  }

  override def get(id: String): DbEffect[Option[Todo]] = DB.lift { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(Sync[F].delay(conn.prepareStatement("select uid, text from item where uid=?")))
      _ = st.setString(1, id)
      rs <- Resource.fromAutoCloseable(Sync[F].delay(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).toList.headOption
    }

    r.use(r => Sync[F].delay(r))
  }

  private def create(todo: Todo): DbEffect[Todo] = DB.lift { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(Sync[F].delay(conn.prepareStatement(
        "insert into item(id, uid, text, created, updated)" +
          " values(nextval('item_id_s'), ?, ?, now(), now()) returning *"
      )))
      _ = st.setString(1, todo.id)
      _ = st.setString(2, todo.text)
      rs <- Resource.fromAutoCloseable(Sync[F].delay(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).next()
    }

    r.use(r => Sync[F].delay(r))
  }

  private def update(todo: Todo): DbEffect[Option[Todo]] = DB.lift { conn =>
    val r = for {
      st <- Resource.fromAutoCloseable(Sync[F].delay(conn.prepareStatement(
        "update item set uid=?, text=? where uid=? returning *"
      )))
      _ = st.setString(1, todo.id)
      _ = st.setString(2, todo.text)
      _ = st.setString(3, todo.id)
      rs <- Resource.fromAutoCloseable(Sync[F].delay(st.executeQuery()))
    } yield {
      rsIterator(rs)(parse).toSeq.headOption
    }

    r.use(r => Sync[F].delay(r))
  }

  private def parse(rs: ResultSet): Todo = Todo(
    id = rs.getString("uid"),
    text = rs.getString("text"))

  private def rsIterator[T](rs: ResultSet)(f: ResultSet => T): Iterator[T] =
    Iterator.continually(rs.next()).takeWhile(identity).map(_ => f(rs))
}
