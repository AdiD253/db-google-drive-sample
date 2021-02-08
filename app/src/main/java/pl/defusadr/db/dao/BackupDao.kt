package pl.defusadr.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import io.reactivex.rxjava3.core.Flowable
import io.reactivex.rxjava3.core.Single
import pl.defusadr.db.entity.SampleEntity

@Dao
internal abstract class BackupDao {

  @Query("SELECT * FROM samples")
  abstract fun getSamples(): Single<List<SampleEntity>>

  @Query("SELECT COUNT(id) FROM samples")
  abstract fun countSamples(): Int

  @Query("SELECT COUNT(id) FROM samples")
  abstract fun subscribeToSamplesChanges(): Flowable<Int>

  @Insert
  abstract fun insertSamples(samples: List<SampleEntity>)


  @Query("DELETE FROM samples")
  abstract fun clearSamples()

  @Transaction
  open fun clearAndInsertSamples(samples: List<SampleEntity>) {
    clearSamples()
    insertSamples(samples)
  }

  @Transaction
  open fun countDatabaseContent(): Int =
    countSamples()
}