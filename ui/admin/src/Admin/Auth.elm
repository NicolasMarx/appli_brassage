module Admin.Auth exposing
    ( Admin
    , loginRequest
    , loginResponseDecoder
    )

import Http
import Json.Decode as D exposing (Decoder)
import Json.Encode as E

-- DOMAIN
type alias Admin =
    { id : String
    , email : String
    , firstName : String
    , lastName : String
    }

-- HELPERS
fieldString : List String -> Decoder String
fieldString names =
    D.oneOf (List.map (\k -> D.field k D.string) names)

nest : List String -> Decoder a -> Decoder a
nest keys dec =
    case keys of
        [] -> dec
        k :: ks -> D.field k (nest ks dec)

-- DECODERS (forme plate OU imbriquée sous "admin"/"data")
adminDecoderFlexible : Decoder Admin
adminDecoderFlexible =
    D.map4 Admin
        (fieldString [ "id", "adminId", "userId" ])
        (fieldString [ "email", "emailAddress" ])
        (fieldString [ "firstName", "first_name", "firstname" ])
        (fieldString [ "lastName", "last_name", "lastname" ])

loginResponseDecoder : Decoder Admin
loginResponseDecoder =
    D.oneOf
        [ adminDecoderFlexible
        , nest [ "admin" ] adminDecoderFlexible
        , nest [ "data", "admin" ] adminDecoderFlexible
        , nest [ "data" ] adminDecoderFlexible
        , nest [ "user" ] adminDecoderFlexible
        ]

-- HTTP
loginRequest : String -> String -> (Result Http.Error Admin -> msg) -> Cmd msg
loginRequest email password toMsg =
    Http.request
        { method = "POST"
        , headers =
            [ Http.header "Content-Type" "application/json"
              -- Décommente si tu n'as PAS bypassé le CSRF côté Play :
              -- , Http.header "Csrf-Token" "nocheck"
            ]
        , url = "/api/admin/login"
        , body =
            Http.jsonBody <|
                E.object
                    [ ( "email", E.string email )
                    , ( "password", E.string password )
                    ]
        , expect = Http.expectJson toMsg loginResponseDecoder
        , timeout = Nothing
        , tracker = Nothing
        }
