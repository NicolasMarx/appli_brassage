module Api exposing
    ( withCsrfHeaders
    , login
    , logout
    , getHops
    , getAromas
    , uploadHopsCsv
    , uploadHopsCsvFile
    , Hop
    , Aroma
    , UploadResult
    , Token
    )

import File exposing (File)
import Http
import Json.Decode as D
import Json.Encode as E


-- HEADERS ----------------------------------------------------------------------

{-| N’ajoute **que** le Csrf-Token. Ne jamais fixer Content-Type ici,
    les bodies (jsonBody/stringBody/fileBody) s’en chargent eux-mêmes.
-}
withCsrfHeaders : Maybe String -> List Http.Header
withCsrfHeaders maybeToken =
    case maybeToken of
        Just t ->
            [ Http.header "Csrf-Token" t ]

        Nothing ->
            []


-- TYPES PARTAGÉS ---------------------------------------------------------------

type alias Hop =
    { id : String
    , name : String
    }

type alias Aroma =
    { id : String
    , name : String
    }

type alias UploadResult =
    ()


-- AUTH -------------------------------------------------------------------------

type alias Token =
    String

login :
    { email : String, password : String }
    -> Maybe String
    -> (Result Http.Error Token -> msg)
    -> Cmd msg
login creds csrf onResult =
    Http.request
        { method = "POST"
        , url = "/api/admin/login"
        , headers = withCsrfHeaders csrf
        , body =
            E.object
                [ ( "email", E.string creds.email )
                , ( "password", E.string creds.password )
                ]
                |> Http.jsonBody
        , expect = Http.expectJson onResult tokenDecoder
        , timeout = Nothing
        , tracker = Nothing
        }


logout : Maybe String -> (Result Http.Error () -> msg) -> Cmd msg
logout csrf onResult =
    Http.request
        { method = "POST"
        , url = "/api/admin/logout"
        , headers = withCsrfHeaders csrf
        , body = Http.emptyBody
        , expect = expectUnit onResult
        , timeout = Nothing
        , tracker = Nothing
        }


tokenDecoder : D.Decoder Token
tokenDecoder =
    D.oneOf
        [ D.field "token" D.string
        , D.field "jwt" D.string
        , D.field "access_token" D.string
        , D.string
        ]


expectUnit : (Result Http.Error () -> msg) -> Http.Expect msg
expectUnit toMsg =
    Http.expectStringResponse toMsg <|
        \response ->
            case response of
                Http.GoodStatus_ _ _ ->
                    Ok ()

                Http.BadStatus_ metadata _ ->
                    Err (Http.BadStatus metadata.statusCode)

                Http.BadUrl_ url ->
                    Err (Http.BadUrl url)

                Http.Timeout_ ->
                    Err Http.Timeout

                Http.NetworkError_ ->
                    Err Http.NetworkError


-- LISTES -----------------------------------------------------------------------

getHops : Maybe String -> (Result Http.Error (List Hop) -> msg) -> Cmd msg
getHops csrf onResult =
    Http.request
        { method = "GET"
        , url = "/api/hops"
        , headers = withCsrfHeaders csrf
        , body = Http.emptyBody
        , expect = Http.expectJson onResult (D.list hopDecoder)
        , timeout = Nothing
        , tracker = Nothing
        }

getAromas : Maybe String -> (Result Http.Error (List Aroma) -> msg) -> Cmd msg
getAromas csrf onResult =
    Http.request
        { method = "GET"
        , url = "/api/aromas"
        , headers = withCsrfHeaders csrf
        , body = Http.emptyBody
        , expect = Http.expectJson onResult (D.list aromaDecoder)
        , timeout = Nothing
        , tracker = Nothing
        }

hopDecoder : D.Decoder Hop
hopDecoder =
    D.map2 Hop
        (D.field "id" D.string)
        (D.field "name" D.string)

aromaDecoder : D.Decoder Aroma
aromaDecoder =
    D.map2 Aroma
        (D.field "id" D.string)
        (D.field "name" D.string)


-- UPLOAD CSV -------------------------------------------------------------------

uploadHopsCsv :
    String -> Maybe String -> (Result Http.Error UploadResult -> msg) -> Cmd msg
uploadHopsCsv csvContent csrf onResult =
    Http.request
        { method = "POST"
        , url = "/api/admin/hops/upload"
        , headers = withCsrfHeaders csrf
        , body = Http.stringBody "text/csv" csvContent
        , expect = expectUnit onResult
        , timeout = Nothing
        , tracker = Nothing
        }

uploadHopsCsvFile :
    File -> Maybe String -> (Result Http.Error UploadResult -> msg) -> Cmd msg
uploadHopsCsvFile file csrf onResult =
    Http.request
        { method = "POST"
        , url = "/api/admin/hops/upload"
        , headers = withCsrfHeaders csrf
        , body = Http.fileBody file
        , expect = expectUnit onResult
        , timeout = Nothing
        , tracker = Nothing
        }
