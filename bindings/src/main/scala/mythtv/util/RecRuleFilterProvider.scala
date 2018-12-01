// SPDX-License-Identifier: LGPL-2.1-only
/*
 * RecRuleFilterProvider.scala
 *
 * Copyright (c) 2016-2018 Tom Grigg <tom@grigg.io>
 */
package mythtv
package util

import services._

class RecRuleFilterProvider(host: String) extends (() => Map[Int, String]) {
  def apply(): Map[Int, String] = {
    // This implementation will only work on 0.28+, when the GetRecRuleFilterList endpoint was added.
    // For prior versions, we must read form the database directly.
    val dvr = ServiceProvider.dvrService(host)
    val filterList = dvr.getRecRuleFilterList.get
    filterList.map(f => (1 << f.id, f.name))(collection.breakOut)
  }
}
