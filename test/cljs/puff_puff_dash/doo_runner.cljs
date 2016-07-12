(ns puff-puff-dash.doo-runner
  (:require [doo.runner :refer-macros [doo-tests]]
            [puff-puff-dash.core-test]))

(doo-tests 'puff-puff-dash.core-test)

