package com.fabahaba.jedipus.primitive;

import com.fabahaba.jedipus.FutureLongReply;
import com.fabahaba.jedipus.FutureReply;
import com.fabahaba.jedipus.exceptions.RedisUnhandledException;

abstract class StatefulFutureReply<T> implements FutureReply<T>, FutureLongReply {

  protected static enum State {
    EMPTY, PENDING, PENDING_DEPENDENCY, BUILDING_DEPENDENCY, BUILDING, BUILT, BROKEN;
  }

  protected State state = State.EMPTY;
  protected RuntimeException exception = null;

  protected StatefulFutureReply<?> execDependency = null;

  public void setException(final RuntimeException exception) {

    this.exception = exception;
    state = State.BROKEN;
  }

  public void setExecDependency(final StatefulFutureReply<?> execDependency) {

    this.execDependency = execDependency;
    state = State.PENDING_DEPENDENCY;
  }

  @Override
  public StatefulFutureReply<T> checkReply() {

    switch (state) {
      case PENDING_DEPENDENCY:
        state = State.BUILDING_DEPENDENCY;
        try {
          // Dependency will drive another build of this after setting this reply.
          execDependency.checkReply();
          return this;
        } catch (final RuntimeException re) {
          setException(re);
          throw re;
        }
      case PENDING:
        state = State.BUILDING;
        try {
          handleReply();
          state = State.BUILT;
          return this;
        } catch (final RuntimeException re) {
          setException(re);
          throw re;
        }
      case EMPTY:
        throw new RedisUnhandledException(null,
            "Close your pipeline or multi block before calling this method.");
      case BROKEN:
        throw exception;
      case BUILDING_DEPENDENCY:
      case BUILDING:
      case BUILT:
      default:
        return this;
    }
  }

  protected void handleReply() {}

  public void setReply(final PrimRedisConn conn) {

    setMultiReply(conn.getReplyNoFlush());
  }

  public void setMultiReply(final Object reply) {}

  public void setMultiLongReply(final long reply) {}

  @Override
  public T get() {

    return null;
  }

  @Override
  public long getLong() {

    return Long.MIN_VALUE;
  }
}
