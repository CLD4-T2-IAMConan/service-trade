-- 테스트용 데이터 초기화 스크립트 (H2 인메모리 DB)
-- DealServiceTest에서 사용하는 테스트 데이터

-- 기존 테스트 데이터 삭제 (선택사항)
DELETE FROM deal WHERE ticket_id IN (4, 5);
DELETE FROM ticket WHERE ticket_id IN (4, 5);

-- 테스트용 티켓 데이터 삽입
-- ID 4: AVAILABLE 상태 (아이유 콘서트)
INSERT INTO ticket (
    ticket_id, event_name, event_date, event_location, owner_id, 
    ticket_status, original_price, selling_price, seat_info, ticket_type, 
    category_id, trade_type, description, created_at, updated_at
) VALUES (
    4, 
    '아이유 콘서트', 
    TIMESTAMP '2024-12-31 19:00:00', 
    '올림픽공원 올림픽홀', 
    100,  -- TEST_SELLER_ID
    'AVAILABLE', 
    100000, 
    120000, 
    'VIP석 1열', 
    'VIP', 
    1, 
    'DELIVERY', 
    '아이유 콘서트 VIP 티켓', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);

-- ID 5: RESERVED 상태 (뮤지컬 위키드)
INSERT INTO ticket (
    ticket_id, event_name, event_date, event_location, owner_id, 
    ticket_status, original_price, selling_price, seat_info, ticket_type, 
    category_id, trade_type, description, created_at, updated_at
) VALUES (
    5, 
    '뮤지컬 위키드', 
    TIMESTAMP '2024-12-25 14:00:00', 
    '디큐브아트센터', 
    100,  -- TEST_SELLER_ID
    'RESERVED', 
    80000, 
    90000, 
    'R석 10열', 
    'R석', 
    2, 
    'ONSITE', 
    '뮤지컬 위키드 R석 티켓', 
    CURRENT_TIMESTAMP, 
    CURRENT_TIMESTAMP
);

