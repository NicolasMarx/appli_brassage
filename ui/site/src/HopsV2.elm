module HopsV2 exposing
    ( Hop
    , decoder
    , formatPct
    , formatMl
    )

import Json.Decode as D
import Set
import String


-- Helpers for building a large decoder without extra packages

andMap : D.Decoder a -> D.Decoder (a -> b) -> D.Decoder b
andMap arg fun =
    D.map2 (\f a -> f a) fun arg

required : String -> D.Decoder a -> D.Decoder a
required key dec =
    D.field key dec

optional : String -> D.Decoder a -> a -> D.Decoder a
optional key dec default =
    D.oneOf [ D.field key dec, D.succeed default ]


-- Common transforms

nonEmpty : String -> Maybe String
nonEmpty s =
    let
        t = String.trim s
    in
    if t == "" then
        Nothing
    else
        Just t

pctToFrac : Maybe Float -> Maybe Float
pctToFrac =
    Maybe.map (\x -> x / 100)

upper : String -> String
upper =
    String.toUpper

cleanupCountry : Maybe String -> Maybe String
cleanupCountry m =
    m |> Maybe.andThen nonEmpty |> Maybe.map upper

-- top-level helper (was illegal inside let with a type annotation)
fracOrNull : String -> D.Decoder (Maybe Float)
fracOrNull key =
    optional key (D.nullable D.float |> D.map pctToFrac) Nothing

-- Split a list of possibly comma-joined strings into flat, trimmed, deduped list
splitTags : List String -> List String
splitTags raw =
    raw
        |> List.concatMap (\s -> String.split "," s)
        |> List.map String.trim
        |> List.filter (\x -> x /= "")
        |> dedupePreserveOrder

dedupePreserveOrder : List String -> List String
dedupePreserveOrder xs =
    let
        step s ( seen, acc ) =
            if Set.member (String.toLower s) seen then
                ( seen, acc )
            else
                ( Set.insert (String.toLower s) seen, acc ++ [ s ] )
    in
    xs |> List.foldl step ( Set.empty, [] ) |> Tuple.second


-- DOMAIN

type alias Hop =
    { id : String
    , name : String
    , country : Maybe String
    , hopType : Maybe String
    , description : Maybe String
    , alphaMin : Maybe Float    -- stored as fraction (0..1)
    , alphaMax : Maybe Float
    , betaMin : Maybe Float     -- fraction (0..1)
    , betaMax : Maybe Float
    , oilsMin : Maybe Float     -- ml/100g
    , oilsMax : Maybe Float     -- ml/100g
    , totalOils : Maybe Float   -- kept for compatibility (unused by API → Nothing)
    , myrceneMin : Maybe Float  -- fraction (0..1)
    , myrceneMax : Maybe Float
    , humuleneMin : Maybe Float
    , humuleneMax : Maybe Float
    , caryophylleneMin : Maybe Float
    , caryophylleneMax : Maybe Float
    , farneseneMin : Maybe Float
    , farneseneMax : Maybe Float
    , aromas : List String
    , beerStyles : List String
    }


-- DECODER (maps your payload exactly + unit normalization)

decoder : D.Decoder Hop
decoder =
    let
        countryDec : D.Decoder (Maybe String)
        countryDec =
            optional "origin" (D.nullable D.string |> D.map cleanupCountry) Nothing

        hopTypeDec : D.Decoder (Maybe String)
        hopTypeDec =
            optional "hopType" (D.nullable D.string |> D.map (Maybe.andThen nonEmpty)) Nothing

        alphaMinDec : D.Decoder (Maybe Float)
        alphaMinDec =
            optional "alphaMin" (D.nullable D.float |> D.map pctToFrac) Nothing

        alphaMaxDec : D.Decoder (Maybe Float)
        alphaMaxDec =
            optional "alphaMax" (D.nullable D.float |> D.map pctToFrac) Nothing

        betaMinDec : D.Decoder (Maybe Float)
        betaMinDec =
            optional "betaMin" (D.nullable D.float |> D.map pctToFrac) Nothing

        betaMaxDec : D.Decoder (Maybe Float)
        betaMaxDec =
            optional "betaMax" (D.nullable D.float |> D.map pctToFrac) Nothing

        oilsMinDec : D.Decoder (Maybe Float)
        oilsMinDec =
            optional "oilsTotalMl100gMin" (D.nullable D.float) Nothing

        oilsMaxDec : D.Decoder (Maybe Float)
        oilsMaxDec =
            optional "oilsTotalMl100gMax" (D.nullable D.float) Nothing

        aromasDec : D.Decoder (List String)
        aromasDec =
            optional "aromas" (D.list D.string |> D.map splitTags) []

        beerStylesDec : D.Decoder (List String)
        beerStylesDec =
            optional "beerStyles" (D.list D.string |> D.map splitTags) []
    in
    D.succeed Hop
        |> andMap (required "id" D.string)
        |> andMap (required "name" D.string)
        |> andMap countryDec
        |> andMap hopTypeDec
        |> andMap (optional "description" (D.nullable D.string) Nothing)
        |> andMap alphaMinDec
        |> andMap alphaMaxDec
        |> andMap betaMinDec
        |> andMap betaMaxDec
        |> andMap oilsMinDec
        |> andMap oilsMaxDec
        |> andMap (D.succeed Nothing) -- totalOils not present; keep for compatibility
        |> andMap (fracOrNull "myrceneMin")
        |> andMap (fracOrNull "myrceneMax")
        |> andMap (fracOrNull "humuleneMin")
        |> andMap (fracOrNull "humuleneMax")
        |> andMap (fracOrNull "caryophylleneMin")
        |> andMap (fracOrNull "caryophylleneMax")
        |> andMap (fracOrNull "farneseneMin")
        |> andMap (fracOrNull "farneseneMax")
        |> andMap aromasDec
        |> andMap beerStylesDec


-- FORMATTERS (UI expects fractions → show as %)

to1 : Float -> String
to1 x =
    let
        n = (toFloat (round (x * 10))) / 10
    in
    String.fromFloat n

formatPct : Maybe Float -> String
formatPct m =
    case m of
        Nothing ->
            "–"

        Just frac ->
            to1 (frac * 100) ++ " %"

formatMl : Maybe Float -> String
formatMl m =
    case m of
        Nothing ->
            "–"

        Just x ->
            to1 x ++ " ml/100g"
