/*
 * Copyright 2019 Google LLC
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 *     * Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer
 * in the documentation and/or other materials provided with the
 * distribution.
 *     * Neither the name of Google LLC nor the names of its
 * contributors may be used to endorse or promote products derived from
 * this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.google.api.gax.tracing;

import static com.google.common.truth.Truth.assertThat;
import static org.mockito.Mockito.mock;

import io.grpc.Context;
import io.opencensus.trace.BlankSpan;
import io.opencensus.trace.Sampler;
import io.opencensus.trace.Span;
import io.opencensus.trace.SpanBuilder;
import io.opencensus.trace.SpanContext;
import io.opencensus.trace.Tracer;
import io.opencensus.trace.unsafe.ContextUtils;
import java.util.List;
import javax.annotation.Nullable;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

@RunWith(JUnit4.class)
public class OpencensusTracerFactoryTest {
  @Rule public final MockitoRule mockitoRule = MockitoJUnit.rule();
  private FakeTracer internalTracer;

  private OpencensusTracerFactory factory;

  @Before
  public void setUp() {
    internalTracer = new FakeTracer();
  }

  @Test
  public void testSpanNamePassthrough() {
    OpencensusTracerFactory factory = new OpencensusTracerFactory(internalTracer, null);

    factory.newTracer(SpanName.of("FakeClient", "FakeMethod"));

    assertThat(internalTracer.lastSpanName).isEqualTo("FakeClient.FakeMethod");
  }

  @Test
  public void testRoot() {
    OpencensusTracerFactory factory = new OpencensusTracerFactory(internalTracer, null);

    Span parentSpan = mock(Span.class);
    Context origContext =
        Context.current().withValue(ContextUtils.CONTEXT_SPAN_KEY, parentSpan).attach();

    try {
      factory.newRootTracer(SpanName.of("FakeClient", "FakeMethod"));
    } finally {
      Context.current().detach(origContext);
    }

    assertThat(internalTracer.lastParentSpan).isEqualTo(BlankSpan.INSTANCE);
  }

  @Test
  public void testChild() {
    OpencensusTracerFactory factory = new OpencensusTracerFactory(internalTracer, null);

    Span parentSpan = mock(Span.class);
    Context origContext =
        Context.current().withValue(ContextUtils.CONTEXT_SPAN_KEY, parentSpan).attach();

    try {
      factory.newTracer(SpanName.of("FakeClient", "FakeMethod"));
    } finally {
      Context.current().detach(origContext);
    }

    assertThat(internalTracer.lastParentSpan).isEqualTo(parentSpan);
  }

  @Test
  public void testSpanNameOverride() {
    OpencensusTracerFactory factory =
        new OpencensusTracerFactory(internalTracer, "OverridenClient");

    factory.newTracer(SpanName.of("FakeClient", "FakeMethod"));

    assertThat(internalTracer.lastSpanName).isEqualTo("OverridenClient.FakeMethod");
  }

  private static class FakeTracer extends Tracer {
    String lastSpanName;
    Span lastParentSpan;

    @Override
    public SpanBuilder spanBuilderWithExplicitParent(String s, @Nullable Span span) {
      lastSpanName = s;
      lastParentSpan = span;
      return new FakeSpanBuilder();
    }

    @Override
    public SpanBuilder spanBuilderWithRemoteParent(String s, @Nullable SpanContext spanContext) {
      lastSpanName = s;
      return new FakeSpanBuilder();
    }
  }

  private static class FakeSpanBuilder extends SpanBuilder {
    @Override
    public SpanBuilder setSampler(Sampler sampler) {
      return this;
    }

    @Override
    public SpanBuilder setParentLinks(List<Span> list) {
      return this;
    }

    @Override
    public SpanBuilder setRecordEvents(boolean b) {
      return this;
    }

    @Override
    public Span startSpan() {
      return BlankSpan.INSTANCE;
    }
  }
}
