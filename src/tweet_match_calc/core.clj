(ns tweet-match-calc.core
  (:require [tweet-match-calc.db.mysql :as mysql]
            [tweet-match-calc.api.twitter :as twitter]
            [tweet-match-calc.calc.morpho :as morpho]
            [tweet-match-calc.calc.leven :as leven]
            [clojure.string :as str]))


;;　テストデータ
;; (morpho/get-tweet-analyze "tamakazura_yuri")
;; (morpho/get-tweet-analyze "doumonnd")

;; (morpho/get-tweet-analyze "kanojo_hoshi_")
;; (morpho/get-tweet-analyze "rikut_rock")
;; (morpho/get-tweet-analyze "BaskeHi")
;; (morpho/get-tweet-analyze "ilikesicp")
;; (morpho/get-tweet-analyze "nobkz")
;; (morpho/get-tweet-analyze "takuya199850")
;; (morpho/get-tweet-analyze "mug_en")
;; (morpho/get-tweet-analyze "charaoyukirin")
;; (morpho/get-tweet-analyze "tetsuya1975m")

;; (morpho/get-tweet-analyze "kareshi_hoshi_")
;; (morpho/get-tweet-analyze "namida1055")
;; (morpho/get-tweet-analyze "0428hrChi")
;; (morpho/get-tweet-analyze "nemukyun1")
;; (morpho/get-tweet-analyze "chomado")
;; (morpho/get-tweet-analyze "Zwei_Megu")
;; (morpho/get-tweet-analyze "xion_2574")
;; (morpho/get-tweet-analyze "ManyaRiko")
;; (morpho/get-tweet-analyze "gyuunyuu_umai")


;; (leven/get-leven-noun "tamakazura_yuri" "doumonnd")

(def boys ["kanojo_hoshi_" "rikut_rock" "BaskeHi" "ilikesicp" "nobkz" "takuya199850" "mug_en" "charaoyukirin" "tetsuya1975m"])
(def girls ["kareshi_hoshi_" "namida1055" "0428hrChi" "nemukyun1" "chomado" "Zwei_Megu" "xion_2574" "ManyaRiko" "gyuunyuu_umai"])

(defn test-leven []
  (for [b boys
        g girls]
    (do
      (Thread/sleep 5000)
      (str b "/" g ":" (leven/get-leven-noun b g))
      )))
