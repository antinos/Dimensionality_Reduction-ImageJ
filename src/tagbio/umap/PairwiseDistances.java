/*
 * BSD 3-Clause License
 * Copyright (c) 2017, Leland McInnes, 2019 Tag.bio (Java port).
 * See LICENSE.txt.
 */
package tagbio.umap;

import tagbio.umap.metric.Metric;
import tagbio.umap.metric.PrecomputedMetric;

/**
 * Compute pairwise distances between instances using a specified metric.
 * @author Sean A. Irvine
 * @author Richard Littin
 */
final class PairwiseDistances {

  // replacement for sklearn.pairwise_distances

  private PairwiseDistances() { }

  static Matrix pairwiseDistances(final Matrix x, final Metric metric) {
    if (PrecomputedMetric.SINGLETON.equals(metric)) {
      return x;
    }
    final int n = x.rows();
    final float[][] distances = new float[n][n];
    for (int k = 0; k < n; ++k) {
      final float[] xk = x.row(k);
      for (int j = 0; j < n; ++j) {
        distances[k][j] = metric.distance(xk, x.row(j));
      }
    }
    return new DefaultMatrix(distances);
  }

  static Matrix pairwiseDistances(final Matrix x, final Matrix y, final Metric metric) {
    if (PrecomputedMetric.SINGLETON.equals(metric)) {
      throw new IllegalArgumentException("Cannot use this method with precomputed");
    }
    final int xn = x.rows();
    final int yn = y.rows();
    final float[][] distances = new float[xn][yn];
    for (int k = 0; k < xn; ++k) {
      final float[] xk = x.row(k);
      for (int j = 0; j < yn; ++j) {
        distances[k][j] = metric.distance(xk, y.row(j));
      }
    }
    return new DefaultMatrix(distances);
  }

}
