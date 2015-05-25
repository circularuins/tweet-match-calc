(ns tweet-match-calc.core
  (:require [tweet-match-calc.db.mysql :as mysql]
            [tweet-match-calc.api.twitter :as twitter]
            [tweet-match-calc.calc.morpho :as morpho]
            [tweet-match-calc.calc.leven :as leven]
            [clojure.string :as str]))


;;　テストデータ
(def n-1 (str/join (morpho/select-noun (twitter/get-tweets "tamakazura_yuri"))))
(def n-2 (str/join (morpho/select-noun (twitter/get-tweets "doumonnd"))))
(def v-1 (str/join (morpho/select-verb (twitter/get-tweets "tamakazura_yuri"))))
(def v-2 (str/join (morpho/select-verb (twitter/get-tweets "doumonnd"))))

(leven/get-leven-noun "tamakazura_yuri" "doumonnd")
