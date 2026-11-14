#!/usr/bin/env bash
# Teste de fumaça: exercita a API em execução, de ponta a ponta, contra um PostgreSQL real.
# Uso: ADMIN_EMAIL=... ADMIN_PASSWORD=... ./scripts/smoke-test.sh [url-base]
# Requer curl e jq. Deve rodar contra um banco vazio.
set -euo pipefail

BASE="${1:-http://localhost:8080}"
STATUS=""
BODY=""

# chamar METODO ROTA [JSON] [TOKEN] -> preenche STATUS e BODY
chamar() {
    local metodo="$1" rota="$2" json="${3:-}" token="${4:-}"
    local args=(-s -o /tmp/smoke-body -w '%{http_code}' -X "$metodo" "$BASE$rota")
    [[ -n "$json" ]] && args+=(-H 'Content-Type: application/json' -d "$json")
    [[ -n "$token" ]] && args+=(-H "Authorization: Bearer $token")
    STATUS=$(curl "${args[@]}")
    BODY=$(cat /tmp/smoke-body)
}

# esperar DESCRICAO STATUS_ESPERADO [FILTRO_JQ VALOR_ESPERADO]
esperar() {
    local descricao="$1" status="$2" filtro="${3:-}" valor="${4:-}"
    if [[ "$STATUS" != "$status" ]]; then
        echo "FALHOU: $descricao -> HTTP $STATUS (esperado $status)"; echo "$BODY"; exit 1
    fi
    if [[ -n "$filtro" ]]; then
        local obtido
        obtido=$(echo "$BODY" | jq -r "$filtro")
        if [[ "$obtido" != "$valor" ]]; then
            echo "FALHOU: $descricao -> $filtro = '$obtido' (esperado '$valor')"; echo "$BODY"; exit 1
        fi
    fi
    echo "ok: $descricao"
}

echo "Aguardando a API em $BASE ..."
for _ in $(seq 1 60); do
    if curl -s -o /dev/null "$BASE/v3/api-docs"; then break; fi
    sleep 2
done

chamar GET /v3/api-docs
esperar "documentação OpenAPI é pública" 200 '.info.title' 'Vendas API'

chamar GET /api/clientes
esperar "rota protegida sem token" 401

chamar POST /api/auth/login "{\"email\":\"$ADMIN_EMAIL\",\"senha\":\"senha-errada\"}"
esperar "login com senha errada" 401

chamar POST /api/auth/login "{\"email\":\"$ADMIN_EMAIL\",\"senha\":\"$ADMIN_PASSWORD\"}"
esperar "login do administrador" 200 '.tipo' 'Bearer'
ADMIN=$(echo "$BODY" | jq -r '.token')

chamar POST /api/clientes '{"nome":"Ana Souza","email":"ana@email.com","cpf":"529.982.247-25","telefone":"11999990000"}' "$ADMIN"
esperar "cadastro de cliente" 201 '.cpf' '52998224725'
CLIENTE=$(echo "$BODY" | jq -r '.id')

chamar POST /api/clientes '{"nome":"Outra Ana","email":"ana@email.com","cpf":"111.444.777-35"}' "$ADMIN"
esperar "e-mail de cliente duplicado" 409

chamar POST /api/clientes '{"nome":"","email":"invalido","cpf":"123"}' "$ADMIN"
esperar "cliente inválido lista os erros por campo" 400 '.erros | has("cpf")' 'true'

chamar GET "/api/clientes?page=0&size=5" "" "$ADMIN"
esperar "listagem paginada de clientes" 200 '.page.totalElements' '1'

chamar POST /api/produtos '{"nome":"Teclado","descricao":"ABNT2","preco":199.90,"estoqueInicial":10}' "$ADMIN"
esperar "cadastro de produto com estoque inicial" 201 '.quantidadeEstoque' '10'
PRODUTO=$(echo "$BODY" | jq -r '.id')

chamar POST /api/pedidos "{\"clienteId\":$CLIENTE,\"itens\":[{\"produtoId\":$PRODUTO,\"quantidade\":2}]}" "$ADMIN"
esperar "criação de pedido calcula o total" 201 '.valorTotal == 399.8' 'true'
PEDIDO=$(echo "$BODY" | jq -r '.id')

chamar GET "/api/produtos/$PRODUTO" "" "$ADMIN"
esperar "pedido baixou o estoque" 200 '.quantidadeEstoque' '8'

chamar POST /api/pedidos "{\"clienteId\":$CLIENTE,\"itens\":[{\"produtoId\":$PRODUTO,\"quantidade\":100}]}" "$ADMIN"
esperar "pedido sem estoque suficiente" 422

chamar GET "/api/produtos/$PRODUTO" "" "$ADMIN"
esperar "pedido recusado não alterou o estoque" 200 '.quantidadeEstoque' '8'

chamar POST "/api/pedidos/$PEDIDO/pagamento" "" "$ADMIN"
esperar "pagamento do pedido" 200 '.status' 'PAGO'

chamar POST "/api/pedidos/$PEDIDO/cancelamento" "" "$ADMIN"
esperar "pedido pago não pode ser cancelado" 422

chamar POST /api/pedidos "{\"clienteId\":$CLIENTE,\"itens\":[{\"produtoId\":$PRODUTO,\"quantidade\":3}]}" "$ADMIN"
esperar "segundo pedido" 201
PEDIDO2=$(echo "$BODY" | jq -r '.id')

chamar POST "/api/pedidos/$PEDIDO2/cancelamento" "" "$ADMIN"
esperar "cancelamento do segundo pedido" 200 '.status' 'CANCELADO'

chamar GET "/api/produtos/$PRODUTO" "" "$ADMIN"
esperar "cancelamento devolveu o estoque" 200 '.quantidadeEstoque' '8'

chamar GET "/api/produtos/$PRODUTO/estoque/movimentacoes" "" "$ADMIN"
esperar "histórico tem 4 movimentações" 200 '.page.totalElements' '4'

chamar GET "/api/pedidos?status=PAGO" "" "$ADMIN"
esperar "filtro de pedidos por status" 200 '.content | length' '1'

HOJE=$(date +%F)
chamar GET "/api/vendas/resumo?inicio=$HOJE&fim=$HOJE" "" "$ADMIN"
esperar "resumo conta só o pedido pago" 200 '.quantidadeVendas' '1'
esperar "resumo soma o valor vendido" 200 '.valorTotal == 399.8' 'true'

chamar GET "/api/vendas/produtos-mais-vendidos?inicio=$HOJE&fim=$HOJE" "" "$ADMIN"
esperar "ranking de produtos mais vendidos" 200 '.[0].quantidadeVendida' '2'

chamar POST /api/auth/registrar '{"nome":"Vendedor","email":"vendedor@email.com","senha":"senha-de-teste"}'
esperar "cadastro de vendedor" 201 '.role' 'VENDEDOR'

chamar POST /api/auth/login '{"email":"vendedor@email.com","senha":"senha-de-teste"}'
esperar "login do vendedor" 200
VENDEDOR=$(echo "$BODY" | jq -r '.token')

chamar GET "/api/produtos?nome=tecl" "" "$VENDEDOR"
esperar "vendedor consulta produtos por nome" 200 '.content | length' '1'

chamar GET "/api/vendas/resumo?inicio=$HOJE&fim=$HOJE" "" "$VENDEDOR"
esperar "vendedor não acessa relatórios" 403

chamar DELETE "/api/clientes/$CLIENTE" "" "$VENDEDOR"
esperar "vendedor não pode excluir" 403

chamar DELETE "/api/clientes/$CLIENTE" "" "$ADMIN"
esperar "cliente com pedidos não pode ser excluído" 409

chamar DELETE "/api/produtos/$PRODUTO" "" "$ADMIN"
esperar "admin desativa produto" 204

chamar GET "/api/produtos" "" "$ADMIN"
esperar "produto desativado sai da listagem" 200 '.content | length' '0'

chamar GET /api/clientes/999 "" "$ADMIN"
esperar "cliente inexistente" 404

echo
echo "Todos os testes de fumaça passaram."
